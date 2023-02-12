package org.virtuslab.bazelsteward.e2e

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.PullRequest
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

abstract class CountingGitHostClient : GitHostClient {
  val openNewPrCalls: MutableList<GitBranch> = ArrayList(10)
  val closeOldPrsCalls: MutableList<GitBranch> = ArrayList(10)

  override fun openNewPR(pr: NewPullRequest) {
    openNewPrCalls.add(pr.branch)
  }
  override fun closePrs(pullRequests: List<PullRequest>) {
    closeOldPrsCalls.addAll(pullRequests.map { it.branch })
  }

  override fun getOpenPRs(): List<PullRequest> {
    return openNewPrCalls.map { PullRequest(it) }
  }
}
