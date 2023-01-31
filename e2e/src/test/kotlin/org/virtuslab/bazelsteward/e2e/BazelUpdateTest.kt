package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.Main
import java.io.File

class BazelUpdateTest : E2EBase() {

  @Test
  @Disabled
  fun `Project without Bazel version`(@TempDir tempDir: File) { // This may actually fail depending on the version of Bazel on PATH
    val testResourcePath = "bazel/trivial"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches = listOf("arrow-core", "arrow-fx-coroutines").map { "$branchRef/$it/1.1.5" } + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Test
  @Disabled
  fun `Project with Bazel version in bazeliskrc file`(@TempDir tempDir: File) {
    val testResourcePath = "bazel/bazeliskrc"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches =
      listOf("arrow-core" to "1.1.5", "arrow-fx-coroutines" to "1.1.5", "bazel" to "5.4.0")
        .map { "$branchRef/${it.first}/${it.second}" } + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Disabled // TODO: Enable this when MavenExtractor exception is handled in the workflow
  @Test
  fun `Project without dependencies`(@TempDir tempDir: File) {
    val testResourcePath = "bazel/bazelOnly"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches = listOf("$branchRef/bazel/5.4.0") + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }
}
