package org.virtuslab.bazelsteward.github

import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.*
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus
import org.virtuslab.bazelsteward.core.common.GitClient
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

class GithubClient private constructor(
  private val baseBranch: String,
  private val gitAuthor: GitClient.GitAuthor,
  repository: String,
  token: String,
  url: String
) : GitHostClient {
  private val github: GitHub = GitHubBuilder().withOAuthToken(token).withEndpoint(url).build()

  private val ghRepository =
    github.getRepository(repository) ?: throw IllegalStateException("Github repository must exist")

  private val bazelPRs: List<GHPullRequest> =
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().toList()

  private val branchToGHPR: Map<String, GHPullRequest> = bazelPRs.associateBy { it.head.ref }

  override fun checkPrStatus(branch: GitBranch): PrStatus {
    return checkPrStatus(branchToGHPR[branch.name])
  }

  override fun openNewPR(pr: NewPullRequest) {
    logger.info { "Creating pull request for ${pr.branch}" }
    ghRepository.createPullRequest(
      pr.title,
      pr.branch.name,
      baseBranch,
      pr.body
    )
  }

  override fun getOpenPRs(): List<PullRequest> {
    val openStatuses = setOf(PrStatus.OPEN_MERGEABLE, PrStatus.OPEN_NOT_MERGEABLE)
    return bazelPRs
      .filter { checkPrStatus(it) in openStatuses }
      .map { PullRequest(GitBranch(it.head.ref)) }
  }

  override fun closePrs(pullRequests: List<PullRequest>) {
    val names = pullRequests.map { it.branch.name }
    bazelPRs.filter { it.head.ref in names }.forEach { it.close() }
  }

  private fun checkPrStatus(pr: GHPullRequest?): PrStatus {
    return if (pr == null)
      PrStatus.NONE
    else if (pr.isMerged)
      PrStatus.MERGED
    else if (pr.state == GHIssueState.CLOSED)
      PrStatus.CLOSED
    else if (pr.listCommits().toList().any { it.commit.author.name != gitAuthor.name })
      PrStatus.OPEN_MODIFIED
    else if (pr.mergeable)
      PrStatus.OPEN_MERGEABLE
    else
      PrStatus.OPEN_NOT_MERGEABLE
  }

  companion object {
    fun getClient(env: Environment, baseBranch: String, gitAuthor: GitClient.GitAuthor): GithubClient {
      val url = env.getOrThrow("GITHUB_API_URL")
      val repository = env.getOrThrow("GITHUB_REPOSITORY")
      val token = env.getOrThrow("GITHUB_TOKEN")
      return GithubClient(baseBranch, gitAuthor, repository, token, url)
    }

    fun getRepoPath(env: Environment): Path {
      val workspace = env.getOrThrow("GITHUB_WORKSPACE")
      return Path(workspace)
    }
  }
}
