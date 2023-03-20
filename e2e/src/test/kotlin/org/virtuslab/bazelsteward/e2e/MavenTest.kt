package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

class MavenTest : E2EBase() {

  @Test
  fun `Maven trivial local test`(@TempDir tempDir: Path) {
    val project = "maven/trivial"
    runBazelSteward(tempDir, project)
    val expectedBranches = expectedBranches(
      "io.arrow-kt/arrow-core" to "1.1.5",
      "io.arrow-kt/arrow-fx-coroutines" to "1.1.5",
      "bazel" to "5.4.0",
      "rules_jvm_external" to "4.5",
      "rules_kotlin" to "v1.7.1",
    )
    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }

  @Test
  fun `Check dependency update not in maven central repository`(@TempDir tempDir: Path) {
    val project = "maven/external"
    runBazelSteward(tempDir, project)
    val expectedBranches = expectedBranchPrefixes("com.7theta/utilis", "bazel", "rules_jvm_external", "rules_kotlin")
    checkBranchesWithoutVersions(tempDir, project, expectedBranches)
  }
}