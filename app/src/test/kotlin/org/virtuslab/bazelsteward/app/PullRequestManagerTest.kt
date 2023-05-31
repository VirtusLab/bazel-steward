package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Ok
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Skipped
import org.virtuslab.bazelsteward.app.provider.PostUpdateHookProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestConfigProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestsLimitsProvider
import org.virtuslab.bazelsteward.config.repo.PullRequestLimits
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.fixture.IntegrationTestBase
import org.virtuslab.bazelsteward.fixture.MockGitPlatform
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import java.nio.file.Path

class PullRequestManagerTest : IntegrationTestBase() {
  @Test
  fun `should create pull request when doesn't exceed limit`(@TempDir tempDir: Path) {
    val branchPrefix = "test-prefix"
    val maxOpenPrs = 3
    val maxUpdatesPerPr = 3
    val limits = PullRequestLimits(maxOpenPrs, maxUpdatesPerPr)
    val config = createPullRequestsConfig(branchPrefix, limits)

    val gitPlatform = mockGitPlatform(
      mapOf(
        GitBranch("test-prefix/1") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/2") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/3") to PrStatus.CLOSED,
        GitBranch("test-prefix/4") to PrStatus.CLOSED,
      ),
    )

    val workspace = prepareWorkspace(tempDir, "dummy")
    val pullRequestManager = createPullRequestManager(config, gitPlatform, workspace)

    val branchName = "test-prefix/group-name/artifact-name/version-test-new"
    val prSuggestion = createPullRequestSuggestion(workspace, branchName)

    runBlocking {
      val results = pullRequestManager.applySuggestions(listOf(prSuggestion))
      results[prSuggestion] shouldBe Ok
    }
  }

  @Test
  fun `shouldn't create pull request when exceeds limit`(@TempDir tempDir: Path) {
    val branchPrefix = "test-prefix"
    val maxOpenPrs = 3
    val maxUpdatesPerPr = 3
    val limits = PullRequestLimits(maxOpenPrs, maxUpdatesPerPr)
    val config = createPullRequestsConfig(branchPrefix, limits)

    val gitPlatform = mockGitPlatform(
      mapOf(
        GitBranch("test-prefix/1") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/2") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/3") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/4") to PrStatus.CLOSED,
      ),
    )

    val workspace = prepareWorkspace(tempDir, "dummy")
    val pullRequestManager = createPullRequestManager(config, gitPlatform, workspace)

    val branchName = "test-prefix/group-name/artifact-name/version-test-new"
    val prSuggestion = createPullRequestSuggestion(workspace, branchName)

    runBlocking {
      val results = pullRequestManager.applySuggestions(listOf(prSuggestion))
      results[prSuggestion] shouldBe Skipped("max open PRs limit reached (${config.limits?.maxOpen})")
    }
  }

  private fun createPullRequestSuggestion(workspace: Path, branchName: String) = PullRequestSuggestion(
    NewPullRequest(
      GitBranch(branchName),
      "Updated group-name:artifact-name to version-test-new",
      "Updates group-name:artifact-name from version-test-old to version-test-new",
      emptyList(),
    ),
    "bazel-steward/group-name/artifact-name/",
    listOf(
      CommitRequest(
        "Updated group-name:artifact-name to version-test-new",
        listOf(
          FileChange(workspace.resolve("WORKSPACE"), 0, 5, "0.9.5"),
        ),
      ),
    ),
    listOf(MavenCoordinates.of("group-name", "artifact-name", "version-test-old")),
  )

  private fun createPullRequestsConfig(branchPrefix: String, limits: PullRequestLimits) = PullRequestsConfig(
    title = "\${group} and \${artifact}",
    body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
    labels = listOf("test-label"),
    branchPrefix = branchPrefix,
    limits = limits,
  )

  private fun mockGitPlatform(map: Map<GitBranch, PrStatus>): MockGitPlatform {
    val gitPlatform = mockGitHostClientWithBranches(map)
    val openStatuses = setOf(PrStatus.OPEN_MERGEABLE, PrStatus.OPEN_NOT_MERGEABLE)
    map.keys.filter { map[it] in openStatuses }
      .forEach { gitPlatform.openNewPr(NewPullRequest(it, "", "", emptyList())) }
    return gitPlatform
  }

  private fun createPullRequestManager(
    config: PullRequestsConfig,
    gitPlatform: MockGitPlatform,
    workspace: Path,
  ): PullRequestManager {
    val git = GitOperations(workspace, "master")

    val postUpdateHooks = PostUpdateHookProvider(emptyList(), emptyList())
    val pushToRemote = false

    val updateAllPullRequests = false
    val pullRequestsConfigProvider = PullRequestConfigProvider(listOf(config), emptyList())
    val limitsProvider =
      PullRequestsLimitsProvider(listOf(config), gitPlatform, updateAllPullRequests, pullRequestsConfigProvider)

    return PullRequestManager(gitPlatform, git, postUpdateHooks, workspace, pushToRemote, limitsProvider)
  }
}
