package org.virtuslab.bazelsteward.github

import arrow.core.getOrElse
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import java.nio.file.Path
import kotlin.io.path.Path

class GithubClient private constructor(private val config: Config, repository: String, token: String, url: String) :
  GitHostClient {
  private val github: GitHub = GitHubBuilder().withOAuthToken(token).withEndpoint(url).build()

  private val ghRepository =
    github.getRepository(repository) ?: throw IllegalStateException("Github repository must exist")

  private val bazelPRs: Set<String> by lazy {
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().asSequence().filterNot { it.isMerged }
      .map { it.head.ref }.filter { it.startsWith(GitBranch.branchPrefix) }.toSet()
  }

  override fun checkIfPrExists(branch: GitBranch) = bazelPRs.contains(branch.name)

  override fun openNewPR(branch: GitBranch): Boolean {
    ghRepository.createPullRequest(
      "Updated ${branch.libraryId.name} to ${branch.version.value}",
      branch.name,
      config.baseBranch,
      ""
    )

    return true
  }

  companion object {

    fun getClient(env: Environment, config: Config): GitHostClient {
      val url = env.get("GITHUB_API_URL").getOrElse { throw RuntimeException() }
      val repository = env.get("GITHUB_REPOSITORY").getOrElse { throw RuntimeException() }
      val token = env.get("GITHUB_TOKEN").getOrElse { throw RuntimeException() }
      return GithubClient(config, repository, token, url)
    }

    fun getRepoPath(env: Environment): Path {
      val workspace = env.get("GITHUB_WORKSPACE").getOrElse { throw RuntimeException() }
      return Path(workspace)
    }
  }
}
