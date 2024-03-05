package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

class BzlModTest : E2EBase() {

  @Test
  fun `bzlmod basic update test`(@TempDir tempDir: Path) {
    val project = "bzlmod/example"
    runBazelSteward(tempDir, project)
    val expectedBranches = expectedBranchPrefixes(
      "bazel",
      "bazel_skylib",
      "rules_java",
      "rules_kotlin",
      "rules_cc",
    )
    checkBranchesWithoutVersions(tempDir, project, expectedBranches)
  }
}
