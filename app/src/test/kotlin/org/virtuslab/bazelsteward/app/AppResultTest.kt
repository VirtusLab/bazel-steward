package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Error
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Ok
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.NewPullRequest

class AppResultTest {
  @Test
  fun `exit code is 0 when all suggestions succeeded or were skipped`() {
    mapOf(pullRequestSuggestion("branch-a") to Ok).exitCode() shouldBe 0
    mapOf(pullRequestSuggestion("branch-b") to PullRequestManager.Result.Skipped("limit")).exitCode() shouldBe 0
  }

  @Test
  fun `exit code is 1 when any suggestion failed`() {
    mapOf(
      pullRequestSuggestion("bazel-steward/failed") to
        Error("git push --set-upstream origin branch --force\nremote: Internal Server Error"),
    ).exitCode() shouldBe 1
  }

  private fun pullRequestSuggestion(branch: String) = PullRequestSuggestion(
    NewPullRequest(GitBranch(branch), "title", "body", emptyList()),
    "bazel-steward/",
    emptyList(),
    emptyList(),
  )
}
