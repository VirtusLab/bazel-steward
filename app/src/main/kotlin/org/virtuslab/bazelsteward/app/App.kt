package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.app.provider.UpdateRulesProvider
import org.virtuslab.bazelsteward.config.repo.RepoConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class App(
  val gitOperations: GitOperations,
  val dependencyKinds: List<DependencyKind<*>>,
  val updateLogic: UpdateLogic,
  val libraryUpdateResolver: LibraryUpdateResolver,
  val pullRequestSuggester: PullRequestSuggester,
  val repoConfig: RepoConfig,
  val updateRulesProvider: UpdateRulesProvider,
  val libraryToTextFilesMapper: LibraryToTextFilesMapper,
  val pullRequestManager: PullRequestManager,
  val workspaceRoot: Path,
) {

  suspend fun run() {
    val workspaceRoot = workspaceRoot
    gitOperations.checkoutBaseBranch()

    val updates = dependencyKinds.mapNotNull { kind ->
      val currentLibraries = resolveAvailableVersionsOfUsedLibraries(kind, workspaceRoot) ?: return@mapNotNull null
      val updateSuggestions = resolveUpdateSuggestions(currentLibraries)
      resolveUpdates(kind, updateSuggestions)
    }.flatten()

    val pullRequestSuggestions = pullRequestSuggester.suggestPullRequests(updates)
    pullRequestManager.applySuggestions(pullRequestSuggestions)
  }

  private suspend fun resolveAvailableVersionsOfUsedLibraries(
    kind: DependencyKind<*>,
    workspaceRoot: Path,
  ): Map<out Library, List<Version>>? {
    return try {
      kind.findAvailableVersions(workspaceRoot)
    } catch (e: Exception) {
      logger.warn {
        "Error happened during detecting available versions for ${kind.name}. " +
          "Skipping this dependency kind..."
      }
      logger.warn("Error details: ${e.message}")
      null
    }
  }

  private fun resolveUpdateSuggestions(currentLibraries: Map<out Library, List<Version>>): List<UpdateSuggestion> {
    val updateSuggestions = currentLibraries.mapNotNull {
      val updateRules = updateRulesProvider.resolveForLibrary(it.key)
      updateLogic.selectUpdate(it.key, it.value, updateRules)
    }
    logger.debug { "UpdateSuggestions: " + updateSuggestions.map { it.currentLibrary.id.name + " to " + it.suggestedVersion.value } }
    return updateSuggestions
  }

  private fun resolveUpdates(
    kind: DependencyKind<*>,
    updateSuggestions: List<UpdateSuggestion>,
  ): List<LibraryUpdate> {
    val heuristics = kind.defaultVersionReplacementHeuristics // TODO: read from config
    val updates = updateSuggestions.mapNotNull { updateSuggestion ->
      val libraryFiles = libraryToTextFilesMapper.map(updateSuggestion.currentLibrary)
      libraryUpdateResolver.resolve(libraryFiles, updateSuggestion, heuristics)
    }
    return updates
  }
}
