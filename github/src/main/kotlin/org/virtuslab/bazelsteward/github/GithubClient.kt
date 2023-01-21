package org.virtuslab.bazelsteward.github

import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

class GithubClient private constructor(private val config: Config, repository: String, token: String, url: String) :
  GitHostClient {
  private val github: GitHub = GitHubBuilder().withOAuthToken(token).withEndpoint(url).build()

  private val ghRepository =
    github.getRepository(repository) ?: throw IllegalStateException("Github repository must exist")

  private val bazelPRs: List<GHPullRequest> =
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().toList()
      .filter { it.head.ref.startsWith(GitBranch.bazelPrefix) }
  private val branchToGHPR: Map<String, GHPullRequest> = bazelPRs.associateBy { it.head.ref }


  override fun checkPrStatus(branch: GitBranch): PrStatus {
    val pr = branchToGHPR[branch.name] ?: return PrStatus.NONE
    return if (pr.isMerged)
      PrStatus.MERGED
    else if (pr.state == GHIssueState.CLOSED)
      PrStatus.CLOSED
    else if (pr.listCommits().toList().any { it.commit.author.name != "github-actions[bot]" })
      PrStatus.OPEN_MODIFIED
    else if (pr.mergeable)
      PrStatus.OPEN_MERGEABLE
    else
      PrStatus.OPEN_NOT_MERGEABLE
  }

  override fun openNewPR(branch: GitBranch) {
    logger.info { "Creating pull request for ${branch.name}" }
    ghRepository.createPullRequest(
      "Updated ${branch.libraryId.name} to ${branch.version.value}",
      branch.name,
      config.baseBranch,
      ""
    )
  }

  override fun closeOldPrs(newBranch: GitBranch) {
    val oldPrs =
      bazelPRs.filter { it.head.ref.startsWith(newBranch.libraryPrefix) }.filterNot { it.head.ref == newBranch.name }
    oldPrs.forEach { it.close() }
  }

  companion object {

    fun getClient(env: Environment, config: Config): GitHostClient {
      val url = env.getOrThrow("GITHUB_API_URL")
      val repository = env.getOrThrow("GITHUB_REPOSITORY")
      val token = env.getOrThrow("GITHUB_TOKEN")
      return GithubClient(config, repository, token, url)
    }

    fun getRepoPath(env: Environment): Path {
      val workspace = env.getOrThrow("GITHUB_WORKSPACE")
      return Path(workspace)
    }
  }
}
