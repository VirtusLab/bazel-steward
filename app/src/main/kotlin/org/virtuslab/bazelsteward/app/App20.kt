package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.BazelStewardGitBranch
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.*
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import java.lang.Exception
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import java.util.stream.Collectors

interface TextFile {
  val path: Path
  val content: String

  private class LazyTextFile(override val path: Path) : TextFile {
    override val content: String
      get() = path.readText()
  }

  companion object {
    fun from(path: Path): TextFile = LazyTextFile(path)
  }
}

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


class FileChangeSuggester {
  fun suggestChanges(
    files: List<TextFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion,
    heuristics: List<Heuristic>
  ): LibraryUpdate? {
    return heuristics.firstNotNullOfOrNull { heuristic ->
      heuristic.apply(files.map { BazelFileSearch.createBazelFile(it.path) }, updateSuggestion)
    }
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

class App20(
  private val gitOperations: GitOperations,
  private val dependencyKinds: List<DependencyKind<*>>,
  private val updateLogic: UpdateLogic,
  private val fileFinder: FileFinder,
  private val fileChangeSuggester: FileChangeSuggester,
  private val pullRequestSuggester: PullRequestSuggester,
  private val gitHostClient: GitHostClient,
  private val appConfig: AppConfig
) {

  suspend fun run() {
    val workspaceRoot = appConfig.path
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

      // TODO apply versioning strategy

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

}