package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.App
import java.io.File

class MavenE2E : E2EBase() {

  @Test
  fun `Maven trivial local test`(@TempDir tempDir: File) {
    val file = loadTest(tempDir, "maven/trivial")
    App.main(args = arrayOf(file.toString()))
    val expectedBranches = listOf("arrow-core", "arrow-fx-coroutines").map { "$branchPrefix/$it/1.1.3" }
    checkForBranches(file, expectedBranches + masterBranch)
  }
}
