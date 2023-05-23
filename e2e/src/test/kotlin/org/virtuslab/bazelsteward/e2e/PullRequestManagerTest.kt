package org.virtuslab.bazelsteward.e2e

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.app.PullRequestManager
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Skipped
import org.virtuslab.bazelsteward.app.PullRequestSuggestion
import org.virtuslab.bazelsteward.app.provider.PostUpdateHookProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestConfigProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestsLimitsProvider
import org.virtuslab.bazelsteward.config.repo.PullRequestLimits
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitPlatform
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import org.virtuslab.bazelsteward.e2e.fixture.MockGitPlatform
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import kotlin.io.path.Path

class PullRequestManagerTest : E2EBase(){
  @Test
  fun `should apply suggestions to pull request suggestions`() {
    val repositoryRoot = Path(".")
    val gitPlatform = mockGitPlatform()
    val git = GitOperations(repositoryRoot, "master")
    val postUpdateHooks = PostUpdateHookProvider(emptyList(), emptyList())
    val pushToRemote = false

    val config = PullRequestsConfig(
        title = "\${group} and \${artifact}",
        body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
        labels = listOf("test-label"),
        branchPrefix = "test-prefix",
        limits = PullRequestLimits(3, 3)
    )

    val pullRequestsConfigProvider = PullRequestConfigProvider(listOf(config), emptyList())
    val limitsProvider = PullRequestsLimitsProvider(listOf(config), gitPlatform, false, pullRequestsConfigProvider)


    val pullRequestManager =
        PullRequestManager(gitPlatform, git, postUpdateHooks, repositoryRoot, pushToRemote, limitsProvider)

    val prSuggestion = PullRequestSuggestion(
      NewPullRequest(GitBranch("bazel-steward/group-name/artefact-name/version-test-new"), "Updated group-name:artefact-name to version-test-new", "Updates group-name:artefact-name from version-test-old to version-test-new", emptyList()),
      "bazel-steward/group-name/artefact-name/",
      listOf(CommitRequest("Updated group-name:artefact-name to version-test-new", emptyList())),
      listOf(MavenCoordinates(MavenLibraryId("group-name","artifact-name"), SimpleVersion("version-test-old")))
    )

    kotlinx.coroutines.runBlocking { val applySuggestions = pullRequestManager.applySuggestions(listOf(prSuggestion))
      applySuggestions.getValue(prSuggestion) shouldBe Skipped("max open PRs limit reached (${config.limits?.maxOpen})")
    }
  }

  private fun mockGitPlatform(): MockGitPlatform {
    val pullRequest1 = PullRequest(GitBranch("test-prefix/1"))
    val pullRequest2 = PullRequest(GitBranch("test-prefix/2"))
    val pullRequest3 = PullRequest(GitBranch("test-prefix/3"))
    val pullRequests: List<PullRequest> = listOf(pullRequest1, pullRequest2, pullRequest3)

    return mockGitHostClientWithBranches(pullRequests, GitPlatform.PrStatus.NONE)
  }
}