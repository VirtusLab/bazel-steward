package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.GitBranch
import kotlin.io.path.readText

class GitOperations(private val config: Config) {
  private val git = GitClient(config.path.toFile())

  suspend fun checkoutBaseBranch() {
    git.checkout(config.baseBranch)
  }

  suspend fun createBranchWithChange(change: FileUpdateSearch.FileChangeSuggestion): GitBranch {
    val branch = fileChangeSuggestionToBranch(change)
    git.checkout(branch.name, true)
    val newContents = change.file.readText()
      .replaceRange(change.position, change.position + change.library.version.value.length, change.newVersion.value)
    change.file.toFile().writeText(newContents)
    git.add(change.file)
    git.commit("Updated ${change.library.id.name} to ${change.newVersion.value}")
    return branch
  }

  suspend fun pushBranchToOrigin(branch: GitBranch, force: Boolean) {
    val branchName = branch.name
    git.checkout(branchName)
    git.push(branchName, force = force)
  }

  companion object {
    fun fileChangeSuggestionToBranch(change: FileUpdateSearch.FileChangeSuggestion) =
      GitBranch(change.library.id, change.newVersion)
  }
}
