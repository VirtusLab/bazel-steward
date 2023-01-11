package org.virtuslab.bazelsteward.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

class GitClient(private val repositoryFile: File) {
  private val quiet = "--quiet"

  suspend fun checkout(target: String, newBranch: Boolean = false) {
    val b = if (newBranch) "-b" else ""
    runGitCommand("git checkout $quiet $b $target")
  }

  suspend fun add(vararg paths: Path) {
    val names = paths.map { it.toString() }.reduce { a, b -> "$a $b" }
    runGitCommand("git add $names")
  }

  suspend fun commit(message: String) {
    runGitCommand("""git commit $quiet -m '$message' """)
  }

  suspend fun push(upstream: String? = null) {
    val setUpstreamCommand = upstream?.let { "--set-upstream origin $it" } ?: ""
    runGitCommand("git push $quiet $setUpstreamCommand")
  }

  suspend fun init(initialBranch: String? = null, bare: Boolean = false) {
    val bareCommand = if (bare) "--bare" else ""
    val branchCommand = initialBranch?.let { "--initial-branch=$it" } ?: ""
    runGitCommand("git init $quiet $bareCommand $branchCommand")
  }

  suspend fun remoteAdd(name: String, urlish: String) {
    runGitCommand("git remote add $name $urlish")
  }

  suspend fun showRef(heads: Boolean): List<String> {
    val headsCommand = if (heads) "--heads" else ""
    val res = runGitCommand("git show-ref $headsCommand")
    return res.split('\n').filter { it.isNotBlank() }.map { it.split(' ', limit = 2)[1] }
  }

  suspend fun runGitCommand(gitCommand: String): String {
    return withContext(Dispatchers.IO) {
      val process = ProcessBuilder("sh", "-c", gitCommand).directory(repositoryFile).start()
        .onExit().await()
      val stdout = process.inputStream.bufferedReader().use { it.readText() }
      val stderr = process.errorStream.bufferedReader().use { it.readText() }
      if (stderr.isBlank()) stdout else throw RuntimeException(stderr)
    }
  }
}
