package org.virtuslab.bazelsteward.github

import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import java.lang.RuntimeException
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

class GithubClient private constructor(
  private val config: Config,
  private val url: String,
  private val repository: String,
  token: String,
  patToken: String? = null
) : GitHostClient {

  private val ghRepository = createClient(token)
  private val ghPatRepository = patToken?.let { createClient(it) }

  private val bazelPRs: List<GHPullRequest> =
    ghRepository.queryPullRequests().state(GHIssueState.ALL).list().toList()
      .filter { it.head.ref.startsWith(GitBranch.bazelPrefix) }
  private val branchToGHPR: Map<String, GHPullRequest> = bazelPRs.associateBy { it.head.ref }

  override fun checkPrStatus(branch: GitBranch): PrStatus {
    return checkPrStatus(branchToGHPR[branch.name])
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

  override fun closePrs(library: LibraryId, filterNotVersion: Version?) {
    val statusesToClose = setOf(PrStatus.OPEN_MERGEABLE, PrStatus.OPEN_NOT_MERGEABLE)
    val oldPrs = bazelPRs
      .filter { it.head.ref.startsWith("${GitBranch.bazelPrefix}/${library.name}") }
      .filterNot { filterNotVersion?.let { version -> it.head.ref.endsWith(version.value) } ?: true }
      .filter { checkPrStatus(it) in statusesToClose }
    oldPrs.forEach { it.close() }
  }

  fun reopenPr(branch: GitBranch) {
    ghPatRepository?.let { repository ->
      val pr = repository.queryPullRequests().state(GHIssueState.OPEN).head(branch.name).list().firstOrNull()
        ?: throw RuntimeException("PR ${branch.name} not found")
      pr.close()
      Thread.sleep(1000)
      pr.reopen()
    }
  }

  private fun checkPrStatus(pr: GHPullRequest?): PrStatus {
    return if (pr == null)
      PrStatus.NONE
    else if (pr.isMerged)
      PrStatus.MERGED
    else if (pr.state == GHIssueState.CLOSED)
      PrStatus.CLOSED
    else if (pr.listCommits().toList().any { it.commit.author.name != config.gitAuthor.name })
      PrStatus.OPEN_MODIFIED
    else
      when (pr.mergeable) {
        null -> PrStatus.OPEN_MODIFIED
        true -> PrStatus.OPEN_MERGEABLE
        false -> PrStatus.OPEN_NOT_MERGEABLE
      }
  }

  private fun createClient(token: String): GHRepository {
    return GitHubBuilder().withOAuthToken(token).withEndpoint(url).build().getRepository(repository)
      ?: throw IllegalStateException("Github repository must exist")
  }

  companion object {

    fun getClient(env: Environment, config: Config): GitHostClient {
      val url = env.getOrThrow("GITHUB_API_URL")
      val repository = env.getOrThrow("GITHUB_REPOSITORY")
      val token = env.getOrThrow("GITHUB_TOKEN")
      val patToken = env["GITHUB_PAT_TOKEN"].let { if (it.isNullOrBlank()) null else it }
      return GithubClient(config, url, repository, token, patToken)
    }

    fun getRepoPath(env: Environment): Path {
      val workspace = env.getOrThrow("GITHUB_WORKSPACE")
      return Path(workspace)
    }
  }
}
