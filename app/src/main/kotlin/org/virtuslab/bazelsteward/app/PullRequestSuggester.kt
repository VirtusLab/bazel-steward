package org.virtuslab.bazelsteward.app

import org.apache.commons.text.StringSubstitutor
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.maven.MavenLibraryId

data class PullRequestSuggestion(
  val description: NewPullRequest,
  val branchPrefix: String,
  val commits: List<CommitRequest>
) {
  val branch: GitBranch
    get() = description.branch
}

data class PullRequestConfig(
  val title: String,
  val body: String,
  val labels: List<String>
)

class PullRequestSuggester(private val provider: PullRequestProvider) {

  fun suggestPullRequests(updates: List<LibraryUpdate>): List<PullRequestSuggestion> {
    return updates.map { update ->
      val versionFrom = update.suggestion.currentLibrary.version
      val versionTo = update.suggestion.suggestedVersion
      val libraryId = update.suggestion.currentLibrary.id
      val branch = BazelStewardGitBranch(libraryId, versionTo)

      val config = provider.resolveForLibrary(update.suggestion.currentLibrary)
      val params = mutableMapOf(
        "versionFrom" to versionFrom,
        "versionTo" to versionTo,
        "libraryId" to libraryId
      )
      if (libraryId is MavenLibraryId) {
        params["group"] = libraryId.group
        params["artifact"] = libraryId.artifact
      }
      val substitutor = StringSubstitutor(params).also { it.isEnableUndefinedVariableException = false }

      val title = substitutor.replace(config.title)
      val body = substitutor.replace(config.body)
      val commits = listOf(CommitRequest(title, update.fileChanges))
      PullRequestSuggestion(
        NewPullRequest(
          branch.gitBranch,
          title,
          body,
          config.labels,
        ),
        branch.prefix,
        commits
      )
    }
  }
}
