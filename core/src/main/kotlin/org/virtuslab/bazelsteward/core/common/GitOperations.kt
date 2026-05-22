package org.virtuslab.bazelsteward.core.common

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.GitBranch
import java.lang.RuntimeException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class FileChange(
  val file: Path,
  val offset: Int,
  val length: Int,
  val replacement: String,
)

data class CommitRequest(
  val message: String,
  val changes: List<FileChange>,
)

private val logger = KotlinLogging.logger {}

class GitOperations(repositoryRoot: Path, private val baseBranch: String) {

  companion object {
    suspend fun resolve(workspaceRoot: Path, baseBranch: String): GitOperations {
      val initial = GitClient(workspaceRoot)
      val repositoryRoot = initial.run("rev-parse", "--show-toplevel").trim().let { Path(it) }
      return GitOperations(repositoryRoot, baseBranch)
    }
  }

  private val git = GitClient(repositoryRoot)

  suspend fun checkoutBaseBranch() {
    git.checkout(baseBranch)
  }

  suspend fun pushBranchToOrigin(branch: GitBranch, force: Boolean) {
    val branchName = branch.name
    git.checkout(branchName)
    try {
      git.push(branchName, force = force)
    } catch (ex: RuntimeException) {
      git.push(branchName, force = true)
    }
  }

  suspend fun createBranchWithCommits(branch: GitBranch, commits: List<CommitRequest>) {
    git.checkout(branch.name, newBranch = true)
    commits.forEach { commit ->
      commit.changes.groupBy { it.file }.forEach { (path, changes) ->
        val contents = path.readText()
        val orderedChanges = changes.sortedBy { it.offset }
        val newContents = removeOverlappingChanges(orderedChanges)
          .fold(Pair(contents, 0)) { (content, offset), replacement ->
            content.replaceRange(
              replacement.offset + offset,
              replacement.offset + offset + replacement.length,
              replacement.replacement,
            ) to (offset + replacement.replacement.length - replacement.length)
          }.first
        path.writeText(newContents)
        git.add(path)
      }
      git.commit(commit.message)
    }
  }

  private fun removeOverlappingChanges(changes: List<FileChange>): List<FileChange> {
    val result = mutableListOf<FileChange>()
    var lastOffset = 0
    var lastLength = 0
    for (change in changes) {
      if (change.offset + change.length <= lastOffset + lastLength) {
        logger.debug { "Skipping overlapping file change: $change. All changes: $changes" }
        continue
      }
      result.add(change)
      lastOffset = change.offset
      lastLength = change.length
    }
    return result
  }

  suspend fun commitSelectedFiles(filesToCommit: List<String>, commitMessage: String) {
    git.add(filesToCommit)
    val noChanges = git.runForResult("diff", "--quiet", "--exit-code", "--cached").isSuccess
    if (noChanges) {
      logger.warn { "No changes to commit" }
    } else {
      git.commit(commitMessage)
    }
  }

  suspend fun squashLastTwoCommits() {
    git.run("reset", "--soft", "HEAD~1")
    git.amend()
  }
}
