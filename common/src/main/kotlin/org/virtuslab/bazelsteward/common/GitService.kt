package org.virtuslab.bazelsteward.common

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.Workspace
import java.io.IOException
import kotlin.io.path.readText

class GitService(workspace: Workspace) {
  //private val ident: PersonIdent = PersonIdent("bazel-steward", "no-reply@github.com")
  private val git = Git.open(workspace.path.toFile())

  fun createBranchWithChange(change: FileUpdateSearch.FileChangeSuggestion): Option<GitBranch> {
    try {
      val branch = GitBranch(change.library.id, change.newVersion)
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

  fun pushBranchToOrigin(branch: String) {
    try {
      git.checkout().setName(branch).call()
      git.push().setRemote("origin").setRefSpecs(RefSpec("$branch:$branch")).call()
    } catch (ex: Exception) {
      when (ex) {
        is GitAPIException, is IOException -> {}
        else -> throw ex
      }
    }

  }
}