package org.virtuslab.bazelsteward.e2e.rules.fixture

import io.kotest.common.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.bazel.rules.RuleLibraryId
import org.virtuslab.bazelsteward.bazel.rules.RuleVersion
import org.virtuslab.bazelsteward.bazel.rules.RulesResolver
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.readText

open class RulesUpdate(
  private val project: String,
  private val expectedVersion: Pair<String, String>,
  private val expectedUrl: String? = null,
  private val expectedSha256: String = "0".repeat(64),
) : E2EBase() {

  @Test
  fun `project with specific rules`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir, project, extraDirs = listOf("rules/base"))
    runBazelStewardWith(workspace) {
      it.withGitHubRulesResolver(ExpectedRuleVersionResolver())
        .withRulesOnly()
    }

    val expectedBranches = expectedBranchPrefixes(expectedVersion.first)
    checkBranchesWithoutVersions(tempDir, project, expectedBranches)

    runBlocking {
      val gitClient = GitClient(workspace)
      val existingBranches = gitClient.showRef(heads = true)
      val branch = existingBranches.find { it.contains(expectedBranches.first()) }!!
      gitClient.checkout(branch)
    }

    if (workspace.resolve("WORKSPACE.expected").exists()) {
      val expectedWorkspace = workspace.resolve("WORKSPACE.expected").readText()
      val actualWorkspace = workspace.resolve("WORKSPACE").readText()
      Assertions.assertThat(actualWorkspace).isEqualTo(expectedWorkspace)
    }
  }

  private inner class ExpectedRuleVersionResolver : RulesResolver {
    override fun resolveRuleVersions(ruleId: RuleLibraryId): List<Version> =
      listOf(
        RuleVersion.create(
          expectedUrl ?: ruleId.downloadUrlFor(expectedVersion.second),
          expectedSha256,
          expectedVersion.second,
          date = Instant.now(),
        ),
      )
  }

  private fun RuleLibraryId.downloadUrlFor(tag: String): String {
    val artifact = artifactName.replace(this.tag, tag)
    return when (this) {
      is RuleLibraryId.ReleaseArtifact -> "https://github.com/$repoName/$ruleName/releases/download/$tag/$artifact"
      is RuleLibraryId.ArchiveTagRuleId -> "https://github.com/$repoName/$ruleName/archive/refs/tags/$artifact"
      is RuleLibraryId.ArchiveRuleId -> "https://github.com/$repoName/$ruleName/archive/$artifact"
    }
  }
}
