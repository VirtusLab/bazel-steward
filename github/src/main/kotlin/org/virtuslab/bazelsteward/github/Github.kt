package org.virtuslab.bazelsteward.github

import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHost
import org.virtuslab.bazelsteward.core.Workspace


class Github(workspace: Workspace) : GitHost {
  val github = workspace.gitHostToken?.let { token -> GitHub.connectUsingOAuth(token) } ?: GitHub.connectAnonymously()
  override fun checkRP(branch: GitBranch) {
    github
  }

  override fun openPR(branch: GitBranch) {
    TODO("Not yet implemented")
  }

}
