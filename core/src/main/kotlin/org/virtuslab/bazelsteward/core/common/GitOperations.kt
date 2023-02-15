package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.GitBranch
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class FileChange(
  val file: Path,
  val offset: Int,
  val length: Int,
  val replacement: String
)

data class CommitRequest(
  val message: String,
  val changes: List<FileChange>
)

class GitOperations(private val appConfig: AppConfig) {
  private val git = GitClient(appConfig.workspaceRoot)

  suspend fun checkoutBaseBranch() {
    git.checkout(appConfig.baseBranch)
  }

  suspend fun pushBranchToOrigin(branch: GitBranch, force: Boolean) {
    val branchName = branch.name
    git.checkout(branchName)
    git.push(branchName, force = force)
  }

  suspend fun createBranchWithChange(branch: GitBranch, commits: List<CommitRequest>) {
    git.checkout(branch.name, newBranch = true)
    commits.forEach { commit ->
      commit.changes.groupBy { it.file }.forEach { (path, changes) ->
        val contents = path.readText()
        val newContents = changes.fold(Pair(contents, 0)) { (content, offset), replacement ->
          content.replaceRange(replacement.offset + offset, replacement.offset + offset + replacement.length, replacement.replacement) to (offset + replacement.replacement.length - replacement.length)
        }.first
        path.writeText(newContents)
        git.add(path)
      }
      git.commit(commit.message)
    }
  }
}
