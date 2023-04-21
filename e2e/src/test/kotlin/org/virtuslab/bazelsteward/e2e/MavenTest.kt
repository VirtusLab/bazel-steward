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
    val expectedBranches = expectedBranchPrefixes(
      "io.arrow-kt/arrow-core",
      "io.arrow-kt/arrow-fx-coroutines",
      "com.7theta/utilis",
      "bazel",
      "rules_jvm_external",
      "rules_kotlin",
      "bazel-skylib",
      "rules_scala",
    )
    checkBranchesWithoutVersions(tempDir, project, expectedBranches)
  }
}
