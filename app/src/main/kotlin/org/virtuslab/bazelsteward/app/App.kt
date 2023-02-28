package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.config.repo.RepoConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver

private val logger = KotlinLogging.logger {}

data class App(
  val gitOperations: GitOperations,
  val dependencyKinds: List<DependencyKind<*>>,
  val updateLogic: UpdateLogic,
  val fileFinder: FileFinder,
  val libraryUpdateResolver: LibraryUpdateResolver,
  val pullRequestSuggester: PullRequestSuggester,
  val gitHostClient: GitHostClient,
  val appConfig: AppConfig,
  val repoConfig: RepoConfig,
  val updateRulesProvider: UpdateRulesProvider
) {

  suspend fun run() {
    val workspaceRoot = appConfig.workspaceRoot
    gitOperations.checkoutBaseBranch()

    val updates = dependencyKinds.mapNotNull { kind ->
      val currentLibraries = try {
        kind.findAvailableVersions(workspaceRoot)
      } catch (e: Exception) {
        logger.warn {
          "Error happened during detecting available versions for ${kind.name}. " +
            "Skipping this dependency kind..."
        }
        logger.catching(e)
        return@mapNotNull null
      }

      val updateSuggestions = currentLibraries.mapNotNull {
        val updateRules = updateRulesProvider.resolveForLibrary(it.key)
        updateLogic.selectUpdate(it.key, it.value, updateRules)
      }
      logger.debug { "UpdateSuggestions: " + updateSuggestions.map { it.currentLibrary.id.name + " to " + it.suggestedVersion.value } }

      val searchPatterns = kind.defaultSearchPatterns // TODO: read overrides from config for given dependency kind
      val files = fileFinder.find(searchPatterns)

      val heuristics = kind.defaultVersionReplacementHeuristics // TODO: read from config

      val updates = updateSuggestions.mapNotNull { updateSuggestion ->
        libraryUpdateResolver.resolve(files, updateSuggestion, heuristics)
      }

      updates
    }.flatten()

    val pullRequestSuggestions = pullRequestSuggester.suggestPullRequests(updates)

    pullRequestSuggestions.forEach { pr ->
      when (val prStatus = gitHostClient.checkPrStatus(pr.branch)) {
        NONE, OPEN_NOT_MERGEABLE -> {
          logger.info { "Creating branch ${pr.branch}, PR status: $prStatus" }
          runCatching {
            gitOperations.createBranchWithChange(pr.branch, pr.commits)
            if (appConfig.pushToRemote) {
              gitOperations.pushBranchToOrigin(pr.branch, force = prStatus == OPEN_NOT_MERGEABLE)
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
        }

        CLOSED, MERGED, OPEN_MERGEABLE, OPEN_MODIFIED -> logger.info { "Skipping ${pr.branch}, PR status: $prStatus" }
      }
    }
  }
}
