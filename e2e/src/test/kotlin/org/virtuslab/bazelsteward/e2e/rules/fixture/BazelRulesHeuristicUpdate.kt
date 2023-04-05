package org.virtuslab.bazelsteward.e2e.rules.fixture

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.bazel.rules.RuleVersion
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

open class BazelRulesHeuristicUpdate(
  private val project: String,
  private val expectedUrl: String,
  private val expectedSha256: String,
  private val expectedVersion: String,
  private val expectedBranch: String,
) : E2EBase() {

  @Test
  fun `project with specific rules`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir, project, extraDirs = listOf("rules/base"))

    val gitHostClient = mockGitHostClientWithStatus(GitHostClient.PrStatus.NONE)
    val githubRulesResolverMock = GithubRulesResolverMock(
      RuleVersion.create(
        expectedUrl,
        expectedSha256,
        expectedVersion,
        null,
      ),
    )

    runBazelStewardWith(workspace) {
      it.withGitHubRulesResolver(githubRulesResolverMock)
        .withGitHostClient(gitHostClient, pushToRemote = false)
        .withRulesOnly()
    }

    val workspaceFile = workspace.resolve("WORKSPACE")
    val resultWorkspaceFile = workspace.resolve("Result_WORKSPACE")
    val expectedBranchName = "$expectedBranch/$expectedVersion"

    checkChangesInBranches(tempDir, project, workspaceFile, resultWorkspaceFile, expectedBranchName)
  }
}
