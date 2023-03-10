package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

open class RulesUpdateTest(
  private val project: String,
  private val expectedBranches: List<String>
) : E2EBase() {

  @Disabled
  @Test
  fun `Project with specific rules`(@TempDir tempDir: Path) {
    runBazelStewardWith(tempDir, project) {
      it.withRulesOnly()
    }

    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }
}
