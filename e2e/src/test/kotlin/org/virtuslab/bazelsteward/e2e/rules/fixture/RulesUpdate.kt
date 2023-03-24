package org.virtuslab.bazelsteward.e2e.rules.fixture

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

open class RulesUpdate(
  private val project: String,
  private vararg val expectedVersions: Pair<String, String>,
) : E2EBase() {

  @Test
  fun `project with specific rules`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir, project, extraDirs = listOf("rules/base"))
    runBazelStewardWith(workspace) {
      it.withRulesOnly()
    }

    val expectedBranches = expectedBranches(*expectedVersions)

    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }
}
