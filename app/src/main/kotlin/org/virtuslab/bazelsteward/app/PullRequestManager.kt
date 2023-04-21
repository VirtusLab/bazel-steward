package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.app.PullRequestsLimits.Result
import org.virtuslab.bazelsteward.app.provider.PostUpdateHookProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestsLimitsProvider
import org.virtuslab.bazelsteward.core.GitPlatform
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.common.CommandRunner
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.HookRunFor
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class PullRequestManager(
  private val gitPlatform: GitPlatform,
  private val git: GitOperations,
  private val postUpdateHooks: PostUpdateHookProvider,
  private val workspaceRoot: Path,
  private val pushToRemote: Boolean,
  private val limitsProvider: PullRequestsLimitsProvider,
) {
  suspend fun applySuggestions(pullRequestSuggestions: List<PullRequestSuggestion>) {
    val limits = limitsProvider.create()
    pullRequestSuggestions.forEach { pr ->
      val prStatus = gitPlatform.checkPrStatus(pr.branch)
      when (val result = limits.canCreateOrUpdate(prStatus)) {
        Result.Ok -> {
          createOrUpdateBranchAndPr(pr, prStatus, limits)
          git.checkoutBaseBranch()
        }
        is Result.Blocked -> {
          logger.info { "Skipping ${pr.branch}: ${result.reason}" }
        }
      }
    }
  }

  private suspend fun createOrUpdateBranchAndPr(
    pr: PullRequestSuggestion,
    prStatus: PrStatus,
    limits: PullRequestsLimits,
  ) {
    runCatching {
      logger.info { "Creating branch ${pr.branch}, current PR status: $prStatus" }
      git.createBranchWithCommits(pr.branch, pr.commits)

      applyPostUpdateHooks(pr)

      if (pushToRemote) {
        git.pushBranchToOrigin(pr.branch, force = prStatus != NONE)
        adjustPullRequests(prStatus, pr, limits)
      }
    }.onFailure { logger.error(it) { "Failed to create branch ${pr.branch}" } }
  }

  private suspend fun applyPostUpdateHooks(pr: PullRequestSuggestion) {
    with(postUpdateHooks.resolveForLibrary(pr.oldLibrary)) {
      if (commands.isNotEmpty()) {
        commands.forEach {
          Thread.sleep(1000)
          CommandRunner.run(listOf("sh", "-c", it), workspaceRoot)
        }
        git.commitSelectedFiles(filesToCommit, commitMessage)
        if (runFor == HookRunFor.Commit) {
          git.squashLastTwoCommits()
        }
      }
    }
  }

  private suspend fun adjustPullRequests(currentPrStatus: PrStatus, pr: PullRequestSuggestion, limits: PullRequestsLimits) {
    val openPr = if (currentPrStatus == NONE) {
      val oldPrs = gitPlatform.getOpenPrs().filter {
        isDifferentPrForTheSameDependency(it, pr) && isUnmodified(it)
      }
      gitPlatform.closePrs(oldPrs)
      limits.registerClosedPrs(oldPrs.size)
      gitPlatform.openNewPr(pr.description).also { limits.registerOpenPr() }
    } else {
      PullRequest(pr.branch)
    }
    gitPlatform.onPrChange(openPr, currentPrStatus)
    limits.registerUpdate()
  }

  private fun isDifferentPrForTheSameDependency(existingPr: PullRequest, newPr: PullRequestSuggestion) =
    existingPr.branch != newPr.branch && existingPr.branch.name.startsWith(newPr.branchPrefix)

  private fun isUnmodified(it: PullRequest) =
    gitPlatform.checkPrStatus(it.branch) != OPEN_MODIFIED
}
