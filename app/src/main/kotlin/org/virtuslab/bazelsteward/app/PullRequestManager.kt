package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.common.GitOperations

private val logger = KotlinLogging.logger {}

class PullRequestManager(
  private val gitHostClient: GitHostClient,
  private val gitOperations: GitOperations,
  private val pushToRemote: Boolean,
  private val updateAllPullRequests: Boolean
) {
  suspend fun applySuggestions(pullRequestSuggestions: List<PullRequestSuggestion>) {
    pullRequestSuggestions.forEach { pr ->
      val prStatus = gitHostClient.checkPrStatus(pr.branch)
      if (canCreateOrUpdate(prStatus)) {
        logger.info { "Creating branch ${pr.branch}, PR status: $prStatus" }
        runCatching {
          gitOperations.createBranchWithChange(pr.branch, pr.commits)
          if (pushToRemote) {
            gitOperations.pushBranchToOrigin(pr.branch, force = prStatus != NONE)
            val openPr = if (prStatus == NONE) {
              val oldPrs = gitHostClient.getOpenPrs().filter {
                it.branch.name.startsWith(pr.branchPrefix) && it.branch != pr.branch
              }.filter { gitHostClient.checkPrStatus(it.branch) != OPEN_MODIFIED }
              gitHostClient.closePrs(oldPrs)
              gitHostClient.openNewPr(pr.description)
            } else {
              PullRequest(pr.branch)
            }
            gitHostClient.onPrChange(openPr, prStatus)
          }
        }.onFailure { logger.error(it) { "Failed to create branch ${pr.branch}" } }
        gitOperations.checkoutBaseBranch()
      } else {
        logger.info { "Skipping ${pr.branch}, PR status: $prStatus" }
      }
    }
  }

  private fun canCreateOrUpdate(prStatus: GitHostClient.PrStatus): Boolean =
    when (prStatus) {
      NONE, OPEN_NOT_MERGEABLE -> true
      OPEN_MERGEABLE, OPEN_MODIFIED -> updateAllPullRequests
      else -> false
    }
}
