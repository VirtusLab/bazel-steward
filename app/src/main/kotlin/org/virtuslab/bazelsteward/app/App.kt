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

typealias AppResult = Map<PullRequestSuggestion, PullRequestManager.Result>

data class App(
  val gitOperations: GitOperations,
  val dependencyKinds: List<DependencyKind<*>>,
  val updateLogic: UpdateLogic,
  val libraryUpdateResolver: LibraryUpdateResolver,
  val pullRequestSuggester: PullRequestSuggester,
  val repoConfig: RepoConfig,
  val updateRulesProvider: UpdateRulesProvider,
  val textFileResolver: TextFileResolver,
  val pullRequestManager: PullRequestManager,
  val workspaceRoot: Path,
) {

  suspend fun run(): Map<PullRequestSuggestion, PullRequestManager.Result> {
    gitOperations.checkoutBaseBranch()

    val updates = dependencyKinds.filter { isKindEnabled(it) }.flatMap { kind ->
      val currentLibraries = resolveAvailableVersionsOfUsedLibraries(kind, workspaceRoot)
      val updateSuggestions = resolveUpdateSuggestions(currentLibraries)
      resolveUpdates(kind, updateSuggestions)
    }

    val pullRequestSuggestions = pullRequestSuggester.suggestPullRequests(updates)
    return pullRequestManager.applySuggestions(pullRequestSuggestions)
  }

  private fun isKindEnabled(kind: DependencyKind<*>): Boolean {
    val enabled = updateRulesProvider.isKindEnabled(kind)
    if (!enabled) {
      logger.info { "Skipping ${kind.name} because it is disabled in the config" }
    }
    return enabled
  }

  private suspend fun resolveAvailableVersionsOfUsedLibraries(
    kind: DependencyKind<*>,
    workspaceRoot: Path,
  ): Map<out Library, List<Version>> {
    return try {
      kind.findAvailableVersions(workspaceRoot, ignoreLibrary)
    } catch (e: Exception) {
      logger.warn {
        "Error happened during detecting available versions for ${kind.name}. " +
          "Skipping this dependency kind..."
      }
      logger.warn("Error details: ${e.message}")
      logger.debug(e) { "Full stacktrace" }
      emptyMap()
    }
  }

  private val ignoreLibrary: (Library) -> Boolean = { library ->
    val ignore = !updateRulesProvider.resolveForLibrary(library).enabled
    if (ignore) {
      logger.info { "Skipping ${library.id} because it is disabled in the config" }
    }
    ignore
  }

  private fun resolveUpdateSuggestions(currentLibraries: Map<out Library, List<Version>>): List<UpdateSuggestion> {
    val suggestions = currentLibraries.mapNotNull { (library, versions) ->
      val updateRules = updateRulesProvider.resolveForLibrary(library)
      updateLogic.selectUpdate(library, versions, updateRules)
    }
    logger.info {
      "Update suggestions (${suggestions.size}): " +
        suggestions.map { "${it.currentLibrary.id} to ${it.suggestedVersion}" }
    }
    return suggestions
  }

  private fun resolveUpdates(
    kind: DependencyKind<*>,
    suggestions: List<UpdateSuggestion>,
  ): List<LibraryUpdate> {
    val heuristics = kind.defaultVersionReplacementHeuristics // TODO: read from config
    return suggestions.mapNotNull { suggestion ->
      val potentialFilesWithLibraryVersion = textFileResolver.resolve(suggestion.currentLibrary)
      libraryUpdateResolver.resolve(potentialFilesWithLibraryVersion, suggestion, heuristics)
    }
  }
}
