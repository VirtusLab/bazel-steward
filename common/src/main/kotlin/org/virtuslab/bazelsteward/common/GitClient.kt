package org.virtuslab.bazelsteward.common

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.RefSpec
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.GitBranch
import java.io.IOException
import kotlin.io.path.readText

class GitClient(private val config: Config) {
  // private val ident: PersonIdent = PersonIdent("bazel-steward", "no-reply@github.com")
  private val git = Git.open(config.path.toFile())

  fun checkoutBaseBranch() {
    git.checkout().setName(config.baseBranch).call()
  }

  fun createBranchWithChange(change: FileUpdateSearch.FileChangeSuggestion): Option<GitBranch> {
    try {
      checkoutBaseBranch()
      val branch = fileChangeSuggestionToBranch(change)
      git.checkout().setName(branch.name).setCreateBranch(true).call()
      val newContents =
        change.file.readText()
          .replaceRange(change.position, change.position + change.library.version.value.length, change.newVersion.value)
      change.file.toFile().writeText(newContents)
      git.add().addFilepattern(change.file.toString()).call()
      git.commit().setMessage("Updated ${change.library.id.name} to ${change.newVersion.value}")
        .call()
      return Some(branch)
    } catch (ex: Exception) {
      when (ex) {
        is GitAPIException, is IOException -> return None
        else -> throw ex
      }
    }
  }

  fun pushBranchToOrigin(branch: GitBranch) {
    try {
      val branchName = branch.name
      git.checkout().setName(branchName).call()
      git.push().setRemote("origin").setRefSpecs(RefSpec("$branchName:$branchName")).call()
    } catch (ex: Exception) {
      when (ex) {
        is GitAPIException, is IOException -> {}
        else -> throw ex
      }
    }
  }

  companion object {
    fun fileChangeSuggestionToBranch(change: FileUpdateSearch.FileChangeSuggestion) =
      GitBranch(change.library.id, change.newVersion)
  }
}
