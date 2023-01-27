package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

class GitClient(private val repositoryFile: File) {
  private val quiet = "--quiet"
  private val git = runBlocking { CommandRunner.run(listOf("sh", "-c", "which git"), repositoryFile).trim() }

  suspend fun checkout(target: String, newBranch: Boolean = false) {
    val b = if (newBranch) "-b" else null
    runGitCommand("checkout", quiet, b, target)
  }

  suspend fun deleteBranch(branchName: String) {
    runGitCommand("branch", quiet, "-D", branchName)
  }

  suspend fun add(vararg paths: Path) {
    val names = paths.map { it.toString() }
    runGitCommand(listOf("add") + names)
  }

  suspend fun commit(message: String) {
    runGitCommand("commit", quiet, "-m", message)
  }

  suspend fun push(branch: String? = null, remote: String = "origin", force: Boolean = false) {
    val upCmd = branch?.let { Triple("--set-upstream", remote, it) }
    val pushList = mutableListOf("push", quiet)
    if (force) {
      pushList.add("--force")
    }
    runGitCommand(pushList + (upCmd?.toList() ?: emptyList()))
  }

  suspend fun init(initialBranch: String? = null, bare: Boolean = false) {
    val bareCmd = if (bare) "--bare" else null
    val branchCmd = initialBranch?.let { "--initial-branch=$it" }
    runGitCommand("init", quiet, bareCmd, branchCmd)
  }

  suspend fun remoteAdd(name: String, urlish: String) {
    runGitCommand("remote", "add", name, urlish)
  }

  suspend fun showRef(heads: Boolean): List<String> {
    val headsCmd = if (heads) "--heads" else null
    val res = runGitCommand("show-ref", headsCmd)
    return res.split('\n').filter { it.isNotBlank() }.map { it.split(' ', limit = 2)[1] }
  }

  suspend fun status() = runGitCommand("status")

  suspend fun configureAuthor(email: String, name: String) {
    runGitCommand("config", "user.email", email)
    runGitCommand("config", "user.name", name)
  }

  suspend fun runGitCommand(vararg gitArgs: String?): String = runGitCommand(gitArgs.toList())

  suspend fun runGitCommand(gitArgs: List<String?>): String {
    return CommandRunner.run(listOf(git) + gitArgs.filterNotNull(), repositoryFile)
  }
}
