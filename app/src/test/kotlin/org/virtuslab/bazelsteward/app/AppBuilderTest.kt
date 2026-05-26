package org.virtuslab.bazelsteward.app

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

  @Test
  fun `ensureWorkspaceIsAcceptable accepts a clean workspace`(@TempDir tempDir: Path) {
    val git = initRepo(tempDir, "main")
    runBlocking {
      AppBuilder.ensureWorkspaceIsAcceptable(git, allowDirtyWorkspace = false)
      AppBuilder.ensureWorkspaceIsAcceptable(git, allowDirtyWorkspace = true)
    }
  }

  @Test
  fun `ensureWorkspaceIsAcceptable rejects a dirty workspace by default`(@TempDir tempDir: Path) {
    val git = initRepo(tempDir, "main")
    tempDir.resolve("README.md").writeText("local edit\n")
    val failure = shouldThrow<AppBuilder.DirtyWorkspaceException> {
      runBlocking { AppBuilder.ensureWorkspaceIsAcceptable(git, allowDirtyWorkspace = false) }
    }
    failure.message shouldContain "README.md"
    failure.message shouldContain "--allow-dirty-workspace"
  }

  @Test
  fun `ensureWorkspaceIsAcceptable allows a dirty workspace when opted in`(@TempDir tempDir: Path) {
    val git = initRepo(tempDir, "main")
    tempDir.resolve("README.md").writeText("local edit\n")
    runBlocking {
      AppBuilder.ensureWorkspaceIsAcceptable(git, allowDirtyWorkspace = true)
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
