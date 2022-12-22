package org.virtuslab.bazelsteward.common

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.PersonIdent
import org.virtuslab.bazelsteward.core.Workspace
import java.io.IOException
import kotlin.io.path.readText

class GitChangeApplier(private val workspace: Workspace) {

  private val ident: PersonIdent = PersonIdent("bazel-steward", "no-reply@github.com")

  fun applyChange(change: FileUpdateSearch.FileChangeSuggestion) {
    try {
      val git = Git.open(workspace.path.toFile())
      val branchName = "bazel-steward/${change.libraryId.name}"
      git.checkout().setName(branchName).setCreateBranch(true).call()
      val newContents =
        change.file.readText().replaceRange(change.position, change.position + change.old.length, change.new)
      change.file.toFile().writeText(newContents)
      git.add().addFilepattern(change.file.toString()).call()
      git.commit().setAuthor(ident).setCommitter(ident).setMessage("Updated ${change.libraryId.name} to ${change.new}")
        .call()
    } catch (ex: Exception) {
      when (ex) {
        is GitAPIException, is IOException -> {}
        else -> throw ex
      }
    }
  }
}