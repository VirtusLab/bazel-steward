package org.virtuslab.bazelsteward.e2e.rules.fixture

import io.kotest.common.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

open class RulesUpdate(
  private val project: String,
  private val expectedVersion: Pair<String, String>,
) : E2EBase() {

  @Test
  fun `project with specific rules`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir, project, extraDirs = listOf("rules/base"))
    runBazelStewardWith(workspace) {
      it.withRulesOnly()
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
}
