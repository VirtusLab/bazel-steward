package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Skipped
import org.virtuslab.bazelsteward.app.PullRequestManager.Result.Ok
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
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.fixture.IntegrationTestBase
import org.virtuslab.bazelsteward.fixture.MockGitPlatform
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Path

class PullRequestManagerTest : IntegrationTestBase() {
  @Test
  fun `should create pull request when doesn't exceed limit`(@TempDir tempDir: Path) {
    val config = PullRequestsConfig(
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
      branchPrefix = "test-prefix",
      limits = PullRequestLimits(3, 3),
    )

    val gitPlatform = mockGitPlatform(
      mapOf(
        GitBranch("test-prefix/1") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/2") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/3") to PrStatus.CLOSED,
        GitBranch("test-prefix/4") to PrStatus.CLOSED,
      )
    )

    val workspace = prepareWorkspace(tempDir, "dummy")
    val pullRequestManager = createPullRequestManager(config, gitPlatform, workspace)

    val prSuggestion = PullRequestSuggestion(
      NewPullRequest(
        GitBranch("bazel-steward/group-name/artifact-name/version-test-new"),
        "Updated group-name:artifact-name to version-test-new",
        "Updates group-name:artifact-name from version-test-old to version-test-new",
        emptyList()
      ),
      "bazel-steward/group-name/artifact-name/",
      listOf(
        CommitRequest(
          "Updated group-name:artifact-name to version-test-new", listOf(
            FileChange(workspace.resolve("WORKSPACE"), 1122, 6, "2.15.0"),
            FileChange(workspace.resolve("WORKSPACE"), 1180, 6, "2.15.0"),
            FileChange(workspace.resolve("WORKSPACE"), 1242, 6, "2.15.0"),
          )
        )
      ),
      listOf(MavenCoordinates(MavenLibraryId("group-name", "artifact-name"), SimpleVersion("version-test-old")))
    )

    runBlocking {
      val results = pullRequestManager.applySuggestions(listOf(prSuggestion))
      results[prSuggestion] shouldBe Ok
    }
  }

  @Test
  fun `shouldn't create pull request when exceeds limit`(@TempDir tempDir: Path) {

    val config = PullRequestsConfig(
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
      branchPrefix = "test-prefix",
      limits = PullRequestLimits(3, 3)
    )

    val gitPlatform = mockGitPlatform(
      mapOf(
        GitBranch("test-prefix/1") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/2") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/3") to PrStatus.OPEN_MERGEABLE,
        GitBranch("test-prefix/4") to PrStatus.CLOSED,
      )
    )

    val workspace = prepareWorkspace(tempDir, "dummy")
    val pullRequestManager = createPullRequestManager(config, gitPlatform, workspace)

    val prSuggestion = PullRequestSuggestion(
      NewPullRequest(
        GitBranch("bazel-steward/group-name/artifact-name/version-test-new"),
        "Updated group-name:artifact-name to version-test-new",
        "Updates group-name:artifact-name from version-test-old to version-test-new",
        emptyList()
      ),
      "bazel-steward/group-name/artifact-name/",
      listOf(
        CommitRequest(
          "Updated group-name:artifact-name to version-test-new", listOf(
            FileChange(workspace.resolve("WORKSPACE"), 1122, 6, "2.15.0"),
            FileChange(workspace.resolve("WORKSPACE"), 1180, 6, "2.15.0"),
            FileChange(workspace.resolve("WORKSPACE"), 1242, 6, "2.15.0"),
          )
        )
      ),
      listOf(MavenCoordinates(MavenLibraryId("group-name", "artifact-name"), SimpleVersion("version-test-old")))
    )

    runBlocking {
      val results = pullRequestManager.applySuggestions(listOf(prSuggestion))
      results[prSuggestion] shouldBe Skipped("max open PRs limit reached (${config.limits?.maxOpen})")
    }
  }

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
    workspace: Path
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
