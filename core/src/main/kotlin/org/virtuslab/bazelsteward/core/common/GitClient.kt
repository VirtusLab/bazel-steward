package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class GitClient(private val repositoryFile: File) {
  private val quiet = "--quiet"
  private val git = runBlocking { runCommand(listOf("sh", "-c", "which git")).trim() }

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
    val args = mutableListOf("push", quiet)
    if (branch != null)
      args.addAll(listOf("--set-upstream", remote, branch))
    if (force)
      args.add("--force")
    runGitCommand(args)
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

  suspend fun getAuthor(): GitAuthor {
    val email = runGitCommand("config", "user.email").trim()
    val name = runGitCommand("config", "user.name").trim()
    return GitAuthor(name, email)
  }

  suspend fun runGitCommand(vararg gitArgs: String?): String = runGitCommand(gitArgs.toList())

  suspend fun runGitCommand(gitArgs: List<String?>): String {
    return runCommand(listOf(git) + gitArgs.filterNotNull())
  }

  private suspend fun runCommand(command: List<String>): String {
    logger.debug { command }
    return withContext(Dispatchers.IO) {
      val process = ProcessBuilder(command).directory(repositoryFile).start()
        .onExit().await()
      val stdout = process.inputStream.bufferedReader().use { it.readText() }
      val stderr = process.errorStream.bufferedReader().use { it.readText() }

      if (process.exitValue() == 0) stdout else throw RuntimeException(
        "${command.joinToString(" ")}\n$stdout\n$stderr"
      )
    }
  }

  companion object {
    data class GitAuthor(val name: String, val email: String)
  }
}
