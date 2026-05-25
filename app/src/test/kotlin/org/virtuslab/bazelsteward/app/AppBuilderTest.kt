package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.common.GitClient
import java.nio.file.Path
import kotlin.io.path.writeText

class AppBuilderTest {
  @Test
  fun `resolveBaseBranch returns the current branch name when on a branch`(@TempDir tempDir: Path) {
    val git = initRepo(tempDir, "main")
    runBlocking {
      AppBuilder.resolveBaseBranch(git) shouldBe "main"
    }
  }

  @Test
  fun `resolveBaseBranch returns the commit sha when HEAD is detached`(@TempDir tempDir: Path) {
    val git = initRepo(tempDir, "main")
    runBlocking {
      val sha = git.run("rev-parse", "HEAD").trim()
      git.run("checkout", "--quiet", "--detach", sha)
      val resolved = AppBuilder.resolveBaseBranch(git)
      resolved shouldMatch Regex("[0-9a-f]{40}")
      resolved shouldBe sha
    }
  }

  private fun initRepo(tempDir: Path, branch: String): GitClient {
    val git = GitClient(tempDir)
    runBlocking {
      git.init(initialBranch = branch)
      git.configureAuthor("bazel-steward@virtuslab.org", "Bazel Steward")
      tempDir.resolve("README.md").writeText("seed\n")
      git.add(tempDir.resolve("README.md"))
      git.commit("seed")
    }
    return git
  }
}
