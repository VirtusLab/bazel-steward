package org.virtuslab.bazelsteward.github

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.common.GitClient
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

class GithubClient private constructor(
  private val url: String,
  private val baseBranch: String,
  private val gitAuthor: GitClient.GitAuthor,
  private val repository: String,
  token: String,
  personalToken: String? = null,

) : GitHostClient {
  private val ghRepository = createClient(token)
  private val ghPatRepository = personalToken?.let { createClient(it) }

  private val bazelPRs: List<GHPullRequest> =
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().toList()

  private val branchToGHPR: Map<String, GHPullRequest> = bazelPRs.associateBy { it.head.ref }

  override fun checkPrStatus(branch: GitBranch): PrStatus {
    return checkPrStatus(branchToGHPR[branch.name])
  }

  override fun openNewPr(pr: NewPullRequest): PullRequest {
    logger.info { "Creating pull request for ${pr.branch}" }
    val newPr = ghRepository.createPullRequest(
      pr.title,
      pr.branch.name,
      baseBranch,
      pr.body,
    )
    if(pr.labels.isNotEmpty()) {
      newPr.addLabels(*pr.labels.toTypedArray())
    }
    return PullRequest(pr.branch)
  }

  override fun getOpenPrs(): List<PullRequest> {
    val openStatuses = setOf(PrStatus.OPEN_MERGEABLE, PrStatus.OPEN_NOT_MERGEABLE)
    return bazelPRs
      .filter { checkPrStatus(it) in openStatuses }
      .map { PullRequest(GitBranch(it.head.ref)) }
  }

  override fun closePrs(pullRequests: List<PullRequest>) {
    val names = pullRequests.map { it.branch.name }
    bazelPRs.filter { it.head.ref in names }.forEach { it.close() }
  }

  override suspend fun onPrChange(pr: PullRequest, prStatusBefore: PrStatus) {
    ghPatRepository?.let { repository ->
      if (prStatusBefore != PrStatus.NONE) {
        delay(10000)
      }
      val ghPr =
        repository.queryPullRequests().state(GHIssueState.OPEN).head(pr.branch.name).list().firstOrNull()
          ?: throw RuntimeException("PR for branch ${pr.branch.name} not found")
      ghPr.close()
      delay(1000)
      ghPr.reopen()
    }
  }

  private fun checkPrStatus(pr: GHPullRequest?): PrStatus {
    return if (pr == null) {
      PrStatus.NONE
    } else if (pr.isMerged) {
      PrStatus.MERGED
    } else if (pr.state == GHIssueState.CLOSED) {
      PrStatus.CLOSED
    } else if (pr.listCommits().toList().any { it.commit.author.name != gitAuthor.name }) {
      PrStatus.OPEN_MODIFIED
    } else {
      when (pr.mergeable) {
        null -> PrStatus.OPEN_MODIFIED
        true -> PrStatus.OPEN_MERGEABLE
        false -> PrStatus.OPEN_NOT_MERGEABLE
      }
    }
  }

  private fun createClient(token: String): GHRepository {
    return GitHubBuilder().withOAuthToken(token).withEndpoint(url).build().getRepository(repository)
      ?: throw IllegalStateException("Github repository must exist")
  }

  companion object {
    fun getClient(env: Environment, baseBranch: String, gitAuthor: GitClient.GitAuthor): GithubClient {
      val url = env.getOrThrow("GITHUB_API_URL")
      val repository = env.getOrThrow("GITHUB_REPOSITORY")
      val token = env.getOrThrow("GITHUB_TOKEN")
      val personalToken = env["PERSONAL_TOKEN"].let { if (it.isNullOrBlank()) null else it }
      return GithubClient(url, baseBranch, gitAuthor, repository, token, personalToken)
    }

    fun getRepoPath(env: Environment): Path {
      val workspace = env.getOrThrow("GITHUB_WORKSPACE")
      return Path(workspace)
    }
  }
}
