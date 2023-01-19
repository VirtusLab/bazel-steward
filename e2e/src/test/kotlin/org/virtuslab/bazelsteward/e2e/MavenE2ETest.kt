package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.Main
import java.io.File

class MavenE2ETest : E2EBase() {

  @Test
  fun `Maven trivial local test`(@TempDir tempDir: File) {
    val testResourcePath = "maven/trivial"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--pushToRemote"))
    val expectedBranches = listOf("arrow-core", "arrow-fx-coroutines").map { "$branchRef/$it/1.1.5" } + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Test
  fun `Check dependency update not in maven central repository`(@TempDir tempDir: File) {
    val testResourcePath = "maven/external"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--pushToRemote"))
    val expectedBranches = listOf("$branchRef/utilis", masterRef)
    checkBranchesWithoutVersions(tempDir, testResourcePath, expectedBranches)
  }
}
