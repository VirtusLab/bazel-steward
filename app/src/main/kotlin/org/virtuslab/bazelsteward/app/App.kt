package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.*
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import java.lang.Exception
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.exists
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.config.ConfigEntry
import org.virtuslab.bazelsteward.core.config.RepoConfig
import org.virtuslab.bazelsteward.core.library.*
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.util.stream.Collectors

sealed interface PathPattern {
  sealed class JavaPathMatcher(pattern: String, prefix: String) : PathPattern {
    val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher(prefix + pattern)
  }

  val value: String

  data class Glob(override val value: String) : JavaPathMatcher(value, "glob:")
  data class Regex(override val value: String) : JavaPathMatcher(value, "regex:")
  data class Exact(override val value: String) : PathPattern
}

private val logger = KotlinLogging.logger {}

interface DependencyKind<LibId : LibraryId> {
  val name: String
  suspend fun findAvailableVersions(workspaceRoot: Path): Map<Library, List<Version>>
  val defaultSearchPatterns: List<PathPattern>
  val defaultVersionDetectionHeuristics: List<Heuristic>
}

// TODO add logging
class FileFinder(private val workspaceRoot: Path) {

  fun find(patterns: List<PathPattern>): List<TextFile> {
    val javaMatchers = patterns.filterIsInstance<PathPattern.JavaPathMatcher>()
    val exactMatchers = patterns.filterIsInstance<PathPattern.Exact>()

    val matchedByPattern = if (javaMatchers.isNotEmpty()) {
      Files.walk(workspaceRoot)
        .filter { path -> javaMatchers.any { it.matcher.matches(path) } }
        .collect(Collectors.toList())
    } else {
      emptyList<Path>()
    }

    val matchedExactly = exactMatchers.map { workspaceRoot.resolve(it.value) }.filter { it.exists() }

    return (matchedExactly + matchedByPattern).map(TextFile::from)
  }
}


class PullRequestSuggester {
  // TODO: extend with grouping logic, labels, commiting strategies, configurable messages etc.
  fun suggestPullRequests(updates: List<LibraryUpdate>): List<PullRequestSuggestion> {
    return updates.map { update ->
      val versionFrom = update.updateSuggestion.currentLibrary.version
      val versionTo = update.updateSuggestion.suggestedVersion
      val libraryId = update.updateSuggestion.currentLibrary.id
      val branch = BazelStewardGitBranch(libraryId, versionTo)
      val title = "Updated $libraryId to $versionTo"
      val body = "Updates $libraryId from $versionFrom to $versionTo"
      val commits = listOf(CommitSuggestion(title, update.fileChanges))
      PullRequestSuggestion(
        branch.gitBranch,
        branch.prefix,
        title,
        body,
        labels = listOf("automatic"),
        commits
      )
    }
  }
}


data class PullRequestSuggestion(
  val branch: GitBranch,
  val branchPrefix: String,
  val title: String,
  val body: String,
  val labels: List<String>,
  val commits: List<CommitSuggestion>
)

class App(
  private val gitOperations: GitOperations,
  private val dependencyKinds: List<DependencyKind<*>>,
  private val updateLogic: UpdateLogic,
  private val fileFinder: FileFinder,
  private val fileChangeSuggester: FileChangeSuggester,
  private val pullRequestSuggester: PullRequestSuggester,
  private val gitHostClient: GitHostClient,
  private val appConfig: AppConfig,
  private val repoConfig: RepoConfig
) {

  suspend fun run() {
    val workspaceRoot = appConfig.workspaceRoot
    gitOperations.checkoutBaseBranch()

    val changeSuggestions = dependencyKinds.mapNotNull { kind ->
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

      val searchPatterns = kind.defaultSearchPatterns // TODO: read from config for given dependency kind
      val files = fileFinder.find(searchPatterns)

      val heuristics = kind.defaultVersionDetectionHeuristics // TODO: read from config

      val fileChanges = updateSuggestions.mapNotNull { updateSuggestion ->
        fileChangeSuggester.suggestChanges(files, updateSuggestion, heuristics)
      }

      fileChanges
    }.flatten()

    val pullRequestSuggestions = pullRequestSuggester.suggestPullRequests(changeSuggestions)

    pullRequestSuggestions.forEach { pr ->
      when (val prStatus = gitHostClient.checkPrStatus(pr.branch)) {
        NONE, OPEN_NOT_MERGEABLE -> {
          logger.info { "Creating branch ${pr.branch}" }
          runCatching {
            gitOperations.createBranchWithChange(pr.branch, pr.commits)
            if (appConfig.pushToRemote) {
              gitOperations.pushBranchToOrigin(pr.branch, force = prStatus == OPEN_NOT_MERGEABLE)
              if (prStatus == NONE) {
                val oldPrs = gitHostClient.getOpenPRs().filter {
                  it.branch.name.startsWith(pr.branchPrefix) && it.branch != pr.branch
                }
                gitHostClient.openNewPR(pr.branch) // TODO pass pr metadata
                gitHostClient.closePrs(oldPrs)
              }
            }
          }.exceptionOrNull()?.let { logger.error("Failed to create branch {}", pr.branch, it) }
          gitOperations.checkoutBaseBranch()
        }
        CLOSED, MERGED, OPEN_MERGEABLE, OPEN_MODIFIED -> logger.info { "Skipping ${pr.branch}" }
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