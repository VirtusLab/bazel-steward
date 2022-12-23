package org.virtuslab.bazelsteward.github

import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient

internal class GithubClient(repository: String, token: String, url: String) : GitHostClient {
  private val github: GitHub = GitHubBuilder().withOAuthToken(token).withEndpoint(url).build()

  private val ghRepository =
    github.getRepository(repository) ?: throw IllegalStateException("Github repository must exist")

  private val bazelPRs: Set<String> by lazy {
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().asSequence().filterNot { it.isMerged }
      .map { it.head.ref }.filter { it.startsWith(GitBranch.branchPrefix) }.toSet()
  }

  override fun checkIfPrExists(branch: GitBranch) = bazelPRs.contains(branch.name)

  override fun openNewPR(branch: GitBranch) = true
}
