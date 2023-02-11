package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.rules.RuleUpdateSearch
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class FileChange(
  val file: Path,
  val offset: Int,
  val length: Int,
  val replacement: String
)

data class CommitSuggestion(
  val message: String,
  val changes: List<FileChange>
)

class GitOperations(private val appConfig: AppConfig) {
  private val git = GitClient(appConfig.path.toFile())

  suspend fun checkoutBaseBranch() {
    git.checkout(appConfig.baseBranch)
  }

  suspend fun createBranchWithChange(change: FileChangeSuggestion): GitBranch {
    val branch = change.branch
    try {
      git.checkout(branch.name, newBranch = true)
    } catch (e: RuntimeException) {
      git.deleteBranch(branch.name)
      git.checkout(branch.name, newBranch = true)
    }

    val newContents = change.file.readText()
      .replaceRange(change.position, change.position + change.library.version.value.length, change.newVersion.value)
    change.file.toFile().writeText(newContents)
    git.add(change.file)
    git.commit("Updated ${change.library.id.name} to ${change.newVersion.value}")
    return branch
  }

  suspend fun createBranchWithChange(change: RuleUpdateSearch.FileChangeSuggestion): GitBranch {
    val branch = fileChangeSuggestionToBranch(change)
    git.checkout(branch.name, true)
    val contents = change.file.readText()
    val newContents = change.patches.fold(Pair(contents, 0)) { (content, offset), replacement ->
      content.replaceRange(replacement.position + offset, replacement.position + offset + replacement.lengthToReplace, replacement.patch) to (offset + replacement.patch.length - replacement.lengthToReplace)
    }.first

    change.file.toFile().writeText(newContents)
    git.add(change.file)
    git.commit("Updated ${change.library.id.name} to ${change.version.value}")
    return branch
  }

  suspend fun pushBranchToOrigin(branch: GitBranch, force: Boolean) {
    val branchName = branch.name
    git.checkout(branchName)
    try {
      git.push(branchName)
    } catch (e: RuntimeException) {
      git.push(branchName, force = true)
    }
  }

  suspend fun createBranchWithChange(branch: GitBranch, commits: List<CommitSuggestion>) {
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

  companion object {
    fun fileChangeSuggestionToBranch(change: FileChangeSuggestion) =
      change.branch

    fun fileChangeSuggestionToBranch(change: RuleUpdateSearch.FileChangeSuggestion) =
      GitBranch("${change.library.id} ${change.version}")
  }
}
