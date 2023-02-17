package org.virtuslab.bazelsteward.e2e

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.PullRequest

abstract class CountingGitHostClient : GitHostClient {
  val openNewPrCalls: MutableList<GitBranch> = ArrayList(10)
  val closeOldPrsCalls: MutableList<GitBranch> = ArrayList(10)

  override fun openNewPr(pr: NewPullRequest): PullRequest {
    openNewPrCalls.add(pr.branch)
    return PullRequest(pr.branch)
  }

  override fun closePrs(pullRequests: List<PullRequest>) {
    closeOldPrsCalls.addAll(pullRequests.map { it.branch })
  }

  override fun getOpenPrs(): List<PullRequest> {
    return openNewPrCalls.map { PullRequest(it) }
  }

  override suspend fun onPrChange(pr: PullRequest, prStatusBefore: GitHostClient.PrStatus) {}
}
