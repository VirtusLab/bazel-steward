package org.virtuslab.bazelsteward.app

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.config.ConfigEntry
import org.virtuslab.bazelsteward.core.config.RepoConfig
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import org.virtuslab.bazelsteward.github.GithubClient
import org.virtuslab.bazelsteward.maven.MavenLibraryId

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
  val repoConfig: RepoConfig
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

      currentLibraries.mapKeys {
        val (versioning, bumping) = getConfigurableSetupForLibrary(it.key)
        if (versioning != it.key.versioningSchema || bumping != it.key.bumpingStrategy) {
          it.key.withVersioningSchema(versioning).withBumpingStrategy(bumping)
        } else {
          it.key
        }
      }

      val updateSuggestions = currentLibraries.mapNotNull { updateLogic.selectUpdate(it.key, it.value) }
      logger.debug { "UpdateSuggestions: " + updateSuggestions.map { it.currentLibrary.id.name + " to " + it.suggestedVersion.value } }

      val searchPatterns = kind.defaultSearchPatterns // TODO: read overrides from config for given dependency kind
      val files = fileFinder.find(searchPatterns)

      val heuristics = kind.defaultVersionDetectionHeuristics // TODO: read from config

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
              if (prStatus == NONE) {
                val oldPrs = gitHostClient.getOpenPRs().filter {
                  it.branch.name.startsWith(pr.branchPrefix) && it.branch != pr.branch
                }
                gitHostClient.openNewPR(pr.description)
                gitHostClient.closePrs(oldPrs)
              }
              if (gitHostClient is GithubClient) {
                if (prStatus == OPEN_NOT_MERGEABLE)
                  delay(10000)
                gitHostClient.reopenPr(pr.branch)
              }
            }
          }.onFailure { logger.error(it) { "Failed to create branch ${pr.branch}" } }
          gitOperations.checkoutBaseBranch()
        }

        CLOSED, MERGED, OPEN_MERGEABLE, OPEN_MODIFIED -> logger.info { "Skipping ${pr.branch}, PR status: $prStatus" }
      }
    }
  }

  private fun getConfigurableSetupForLibrary(library: Library): Pair<VersioningSchema, BumpingStrategy> {
    return when (val libraryId = library.id) {
      is MavenLibraryId -> {
        val versioningForDependency = getConfigEntryFromConfigs(libraryId, repoConfig.maven.configs.filter { it.versioning != null })
        val bumpingForDependency = getConfigEntryFromConfigs(libraryId, repoConfig.maven.configs.filter { it.bumping != null })
        Pair(
          versioningForDependency?.versioning ?: VersioningSchema.Loose,
          bumpingForDependency?.bumping ?: BumpingStrategy.Default
        )
      }

      else -> Pair(VersioningSchema.Loose, BumpingStrategy.Minor)
    }
  }

  private fun getConfigEntryFromConfigs(libraryId: MavenLibraryId, configs: List<ConfigEntry>): ConfigEntry? =
    configs.firstOrNull { it.group == libraryId.group && it.artifact == libraryId.artifact }
      ?: configs.firstOrNull { it.group == libraryId.group && it.artifact == null }
      ?: configs.firstOrNull { it.group == null && it.artifact == null }
}
