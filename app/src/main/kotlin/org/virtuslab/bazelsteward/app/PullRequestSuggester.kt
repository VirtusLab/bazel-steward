package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.common.CommitSuggestion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate

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