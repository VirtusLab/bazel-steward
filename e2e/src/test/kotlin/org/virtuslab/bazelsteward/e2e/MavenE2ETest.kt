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
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches =
      listOf("io.arrow-kt/arrow-core" to "1.1.5", "io.arrow-kt/arrow-fx-coroutines" to "1.1.5", "bazel" to "5.3.2")
        .map { "$branchRef/${it.first}/${it.second}" } + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Test
  fun `Check dependency update not in maven central repository`(@TempDir tempDir: File) {
    val testResourcePath = "maven/external"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches = listOf("$branchRef/com.7theta/utilis", "$branchRef/bazel/5.3.2", masterRef)
    checkBranchesWithoutVersions(tempDir, testResourcePath, expectedBranches)
  }
}
