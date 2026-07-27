package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

class MavenBomTest : E2EBase() {

  @Test
  fun `should bump a bom and not the artifacts it manages`(@TempDir tempDir: Path) {
    val project = "maven/bom"
    val workspace = prepareWorkspace(tempDir, project)
    runBazelStewardWith(workspace) {
      it.withMavenOnly().withMockMaven {
        withVersion("com.fasterxml.jackson:jackson-bom", "2.15.2")
        withVersion("com.fasterxml.jackson.core:jackson-core", "2.15.2")
        withVersion("com.google.code.gson:gson", "2.10.2")
      }
    }
    // jackson-core is declared without a version, so it must not get a branch
    // of its own -- the bom is where its version lives.
    val expectedBranches = expectedBranches(
      "com.fasterxml.jackson/jackson-bom" to "2.15.2",
      "com.google.code.gson/gson" to "2.10.2",
    )
    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }
}
