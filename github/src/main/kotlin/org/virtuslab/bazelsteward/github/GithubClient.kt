package org.virtuslab.bazelsteward.github

import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient


internal class GithubClient(repository: String, token: String, url: String) : GitHostClient {
  private val github: GitHub = GitHubBuilder().run {
    withOAuthToken(token)
    withEndpoint(url)
    build()
  }

  private val ghRepository =
    github.getRepository(repository) ?: throw IllegalStateException("Github repository must exist")

  override fun checkRP(branch: GitBranch) {
    ghRepository.queryPullRequests().run{
      state(GHIssueState.ALL)

    }
  }

  override fun openPR(branch: GitBranch) {
    TODO("Not yet implemented")
  }

}
