package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.GitBranch
import kotlin.io.path.readText

class GitOperations(private val config: Config) {
  private val git = GitClient(config.path.toFile())

  suspend fun checkoutBaseBranch() {
    git.checkout(config.baseBranch)
  }

  suspend fun createBranchWithChange(change: FileUpdateSearch.FileChangeSuggestion): GitBranch {
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

  suspend fun pushBranchToOrigin(branch: GitBranch, force: Boolean) {
    val branchName = branch.name
    git.checkout(branchName)
    try {
      git.push(branchName)
    } catch (e: RuntimeException) {
      git.push(branchName, force = true)
    }
  }
}
