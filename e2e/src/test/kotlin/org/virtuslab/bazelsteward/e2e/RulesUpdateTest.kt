package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RulesUpdateTest : E2EBase() {

  @Test
  fun `Project with rules`(@TempDir tempDir: Path) {
    val project = "rules/trivial"
    runBazelSteward(tempDir, project)
    val expectedBranches =
      expectedBranches(
        "io.arrow-kt/arrow-core" to "1.1.5",
        "io.arrow-kt/arrow-fx-coroutines" to "1.1.5",
        "bazel" to "5.3.2",
        "rules_jvm_external" to "4.5"
      )

    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }
}
