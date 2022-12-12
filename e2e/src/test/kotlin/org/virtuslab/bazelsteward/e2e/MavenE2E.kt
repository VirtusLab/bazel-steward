package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.App
import java.io.File

class MavenE2E : E2EBase() {

  @Test
  fun `Maven basic test`(@TempDir tempDir: File) {
    loadTest(tempDir, "maven/trivial")
    App.main(args = arrayOf("main", tempDir.toString()))
  }
}
