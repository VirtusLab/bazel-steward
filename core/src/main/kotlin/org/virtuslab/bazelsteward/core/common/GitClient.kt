package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.runBlocking
import java.nio.file.Path

class GitClient(private val repositoryRoot: Path) {
  private val quiet = "--quiet"

  companion object {
    private val git = runBlocking { CommandRunner.runForOutput(listOf("sh", "-c", "which git")).trim() }
  }

  suspend fun checkout(target: String, newBranch: Boolean = false) {
    val b = if (newBranch) "-b" else null
    run("checkout", quiet, b, target)
  }

  suspend fun deleteBranch(branchName: String) {
    run("branch", quiet, "-D", branchName)
  }

  suspend fun add(vararg paths: Path) {
    val names = paths.map { it.toString() }
    run(listOf("add") + names)
  }

  suspend fun add(paths: List<String>) {
    run(listOf("add") + paths)
  }

  suspend fun commit(message: String) {
    run("commit", quiet, "-m", message)
  }

  suspend fun amend() {
    run("commit", quiet, "--amend", "--no-edit")
  }

  suspend fun push(branch: String? = null, remote: String = "origin", force: Boolean = false) {
    val args = mutableListOf("push", quiet)
    if (branch != null) {
      args.addAll(listOf("--set-upstream", remote, branch))
    }
    if (force) {
      args.add("--force")
    }
    run(args)
  }

  suspend fun init(initialBranch: String? = null, bare: Boolean = false) {
    val bareCmd = if (bare) "--bare" else null
    val branchCmd = initialBranch?.let { "--initial-branch=$it" }
    run("init", quiet, bareCmd, branchCmd)
  }

  suspend fun remoteAdd(name: String, urlish: String) {
    run("remote", "add", name, urlish)
  }

  suspend fun showRef(heads: Boolean): List<String> {
    val headsCmd = if (heads) "--heads" else null
    val res = run("show-ref", headsCmd)
    return res.split('\n').filter { it.isNotBlank() }.map { it.split(' ', limit = 2)[1] }
  }

  suspend fun status() = run("status")

  suspend fun configureAuthor(email: String, name: String) {
    run("config", "user.email", email)
    run("config", "user.name", name)
  }

  suspend fun getAuthor(): GitAuthor {
    val email = run("config", "user.email").trim()
    val name = run("config", "user.name").trim()
    return GitAuthor(name, email)
  }

  suspend fun run(vararg gitArgs: String?): String = run(gitArgs.toList())

  suspend fun run(gitArgs: List<String?>): String {
    return CommandRunner.runForOutput(listOf(git) + gitArgs.filterNotNull(), repositoryRoot)
  }

  suspend fun runForResult(vararg args: String): CommandRunner.Result {
    return CommandRunner.run(listOf(git) + args, repositoryRoot)
  }

  data class GitAuthor(val name: String, val email: String)
}
