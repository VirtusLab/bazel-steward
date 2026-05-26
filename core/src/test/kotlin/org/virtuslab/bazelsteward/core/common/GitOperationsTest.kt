package org.virtuslab.bazelsteward.core.common

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.GitBranch
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GitOperationsTest {

  @Test
  fun `consecutive branches start from the base commit even when HEAD is detached`(@TempDir tempDir: Path) {
    runBlocking {
      val git = GitClient(tempDir)
      git.init(initialBranch = "main")
      git.configureAuthor("bazel-steward@virtuslab.org", "Bazel Steward")

      val file = tempDir.resolve("MODULE.bazel")
      file.writeText("AAAAA\nBBBBB\n")
      git.add(file)
      git.commit("seed")

      val baseSha = git.run("rev-parse", "HEAD").trim()
      git.run("checkout", "--quiet", "--detach", baseSha)

      val operations = GitOperations(tempDir, baseSha)

      val firstChange = FileChange(file, 0, 5, "11111")
      val secondChange = FileChange(file, 6, 5, "22222")

      operations.createBranchWithCommits(
        GitBranch("bazel-steward/first"),
        listOf(CommitRequest("first", listOf(firstChange))),
      )
      file.writeText(file.readText() + "uncommitted noise\n")

      operations.createBranchWithCommits(
        GitBranch("bazel-steward/second"),
        listOf(CommitRequest("second", listOf(secondChange))),
      )

      file.readText() shouldBe "AAAAA\n22222\n"
    }
  }
}
