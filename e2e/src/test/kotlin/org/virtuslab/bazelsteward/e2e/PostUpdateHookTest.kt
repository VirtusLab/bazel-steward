package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.common.CommandRunner
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path

class PostUpdateHookTest : E2EBase() {

  @Test
  fun `Post update hook with PR`(@TempDir tempDir: Path) {
    val project = "hook/pr"
    runBazelStewardWith(tempDir, project) {
      it.withMavenOnly(listOf("1.1.5"))
    }

    val localRoot = tempDir.resolve("local").resolve(project)
    val git = GitClient(localRoot)
    runBlocking {
      git.checkout("bazel-steward/io.arrow-kt/arrow-core/1.1.5")
      val validationCommandOutput = CommandRunner.run(
        localRoot,
        "sh",
        "-c",
        """cat garbage/* rubbish/* trash/* | python3 -c "import sys; print(sum(int(l) for l in sys.stdin))"""",
      ).trim()
      validationCommandOutput shouldBe "45"
      val lastCommit = git.run("log", "-1", "--oneline")
      lastCommit shouldContain "Test message"
      git.checkout(master)
    }

    val expectedBranches = expectedBranches(
      "io.arrow-kt/arrow-core" to "1.1.5",
    )
    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }

  @Test
  fun `Post update hook with commit`(@TempDir tempDir: Path) {
    val project = "hook/commit"
    runBazelStewardWith(tempDir, project) {
      it.withMavenOnly(listOf("1.1.5"))
    }

    val localRoot = tempDir.resolve("local").resolve(project)
    val git = GitClient(localRoot)
    runBlocking {
      git.checkout("bazel-steward/io.arrow-kt/arrow-core/1.1.5")
      val validationCommandOutput = CommandRunner.run(
        localRoot,
        "sh",
        "-c",
        """cat garbage/* rubbish/* trash/* | python3 -c "import sys; print(sum(int(l) for l in sys.stdin))"""",
      ).trim()
      validationCommandOutput shouldBe "45"
      val lastCommit = git.run("log", "-1", "--oneline")
      lastCommit shouldContain "Updated io.arrow-kt:arrow-core to 1.1.5"
      git.checkout(master)
    }

    val expectedBranches = expectedBranches(
      "io.arrow-kt/arrow-core" to "1.1.5",
    )
    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }
}
