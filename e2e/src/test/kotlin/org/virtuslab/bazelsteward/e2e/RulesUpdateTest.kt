package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.Main
import java.io.File

class RulesUpdateTest : E2EBase() {

  @Test
  fun `Project with rules`(@TempDir tempDir: File) {
    val testResourcePath = "rules/trivial"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches =
      listOf("arrow-core" to "1.1.5", "arrow-fx-coroutines" to "1.1.5", "bazel" to "5.3.2", "rules_jvm_external" to "4.5")
        .map { "$branchRef/${it.first}/${it.second}" } + masterRef

    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }
}
