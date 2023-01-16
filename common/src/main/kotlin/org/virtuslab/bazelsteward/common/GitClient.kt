package org.virtuslab.bazelsteward.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

class GitClient(private val repositoryFile: File) {
  private val quiet = "--quiet"
  private val git = runBlocking { runCommand(listOf("sh", "-c", "which git")).trim() }

  suspend fun checkout(target: String, newBranch: Boolean = false) {
    val b = if (newBranch) "-b" else null
    runGitCommand("checkout", quiet, b, target)
  }

  suspend fun add(vararg paths: Path) {
    val names = paths.map { it.toString() }
    runGitCommand(listOf("add") + names)
  }

  suspend fun commit(message: String) {
    runGitCommand("commit", quiet, "-m", message)
  }

  suspend fun push(setUpstream: String? = null) {
    val upCmd = setUpstream?.let { Triple("--set-upstream", "origin", it) }
    runGitCommand(listOf("push", quiet) + (upCmd?.toList() ?: emptyList()))
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

  suspend fun runGitCommand(vararg gitArgs: String?): String = runGitCommand(gitArgs.toList())

  suspend fun runGitCommand(gitArgs: List<String?>): String {
    return runCommand(listOf(git) + gitArgs.filterNotNull())
  }

  private suspend fun runCommand(command: List<String>): String {
    return withContext(Dispatchers.IO) {
      val process = ProcessBuilder(command).directory(repositoryFile).start()
        .onExit().await()
      val stdout = process.inputStream.bufferedReader().use { it.readText() }
      val stderr = process.errorStream.bufferedReader().use { it.readText() }
      if (stderr.isBlank()) stdout else throw RuntimeException(stderr)
    }
  }
}