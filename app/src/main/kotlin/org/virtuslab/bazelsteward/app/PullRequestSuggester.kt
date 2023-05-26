package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.app.provider.PullRequestConfig
import org.virtuslab.bazelsteward.app.provider.PullRequestConfigProvider
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.GroupId
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.maven.MavenLibraryId

data class PullRequestSuggestion(
  val description: NewPullRequest,
  val branchPrefix: String,
  val commits: List<CommitRequest>,
  val oldLibraries: List<Library>,
) {
  val branch: GitBranch
    get() = description.branch
}

@Suppress("NAME_SHADOWING")
class PullRequestSuggester(private val provider: PullRequestConfigProvider) {

  fun suggestPullRequests(updates: List<LibraryUpdate>): List<PullRequestSuggestion> {
    return updates
      .groupBy { update -> provider.resolveGroup(update.suggestion.currentLibrary) }
      .flatMap { (group, updates) ->
        if (group != null) {
          listOf(suggestForGroup(group, updates))
        } else {
          updates.map { suggestForLibrary(it) }
        }
      }
  }

  private fun suggestForGroup(group: GroupId, updates: List<LibraryUpdate>): PullRequestSuggestion {
    return suggest(
      config = provider.resolveForGroup(group),
      libraryId = group,
      updates = updates,
    )
  }

  private fun suggestForLibrary(update: LibraryUpdate): PullRequestSuggestion {
    return suggest(
      config = provider.resolveForLibrary(update.suggestion.currentLibrary),
      libraryId = update.suggestion.currentLibrary.id,
      updates = listOf(update),
    )
  }

  private fun suggest(config: PullRequestConfig, libraryId: LibraryId, updates: List<LibraryUpdate>): PullRequestSuggestion {
    fun resolveVersion(f: (UpdateSuggestion) -> Version): Version {
      return updates.map { f(it.suggestion) }.distinct().singleOrNull() ?: SimpleVersion("mixed")
    }

    val versionFrom = resolveVersion { it.currentLibrary.version }
    val versionTo = resolveVersion { it.suggestedVersion }

    val templateApplier = prepareSubstitutions(libraryId, versionFrom, versionTo, updates)
    val title = templateApplier.apply(config.titleTemplate)
    val body = templateApplier.apply(config.bodyTemplate)
    val prefix = config.branchPrefix

    val branch = BazelStewardGitBranch(prefix, libraryId, versionTo)

    val commit = CommitRequest(title, updates.flatMap { it.fileChanges }.distinct())

    return PullRequestSuggestion(
      NewPullRequest(branch.gitBranch, title, body, config.labels),
      branch.prefix,
      commits = listOf(commit),
      oldLibraries = updates.map { it.suggestion.currentLibrary },
    )
  }

  private fun prepareSubstitutions(
    libraryId: LibraryId,
    versionFrom: Version,
    versionTo: Version,
    updates: List<LibraryUpdate>,
  ): TemplateApplier {
    val updatesString = updates.joinToString("\n") { update ->
      val dependencyId = update.suggestion.currentLibrary.id.name
      val versionFrom = update.suggestion.currentLibrary.version
      val versionTo = update.suggestion.suggestedVersion
      "$dependencyId from $versionFrom to $versionTo"
    }

    val params = mutableMapOf(
      "versionFrom" to versionFrom.value,
      "versionTo" to versionTo.value,
      "dependencyId" to libraryId.name,
      "updates" to updatesString,
    )

    if (libraryId is MavenLibraryId) {
      params["group"] = libraryId.group
      params["artifact"] = libraryId.artifact
    }

    return TemplateApplier(params)
  }
}
