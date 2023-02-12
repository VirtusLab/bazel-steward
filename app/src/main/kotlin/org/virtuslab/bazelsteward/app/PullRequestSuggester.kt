package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate

data class PullRequestSuggestion(
  val description: NewPullRequest,
  val branchPrefix: String,
  val commits: List<CommitRequest>
) {
  val branch: GitBranch
    get() = description.branch
}

class PullRequestSuggester {
  // TODO: extend with grouping logic, labels, commiting strategies, configurable messages etc.
  fun suggestPullRequests(updates: List<LibraryUpdate>): List<PullRequestSuggestion> {
    return updates.map { update ->
      val versionFrom = update.suggestion.currentLibrary.version
      val versionTo = update.suggestion.suggestedVersion
      val libraryId = update.suggestion.currentLibrary.id
      val branch = BazelStewardGitBranch(libraryId, versionTo)
      val title = "Updated $libraryId to $versionTo"
      val body = "Updates $libraryId from $versionFrom to $versionTo"
      val commits = listOf(CommitRequest(title, update.fileChanges))
      PullRequestSuggestion(
        NewPullRequest(
          branch.gitBranch,
          title,
          body,
          labels = listOf("automatic"),
        ),
        branch.prefix,
        commits
      )
    }
  }
}
