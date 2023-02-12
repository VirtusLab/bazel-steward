package org.virtuslab.bazelsteward.core

data class PullRequest(val branch: GitBranch)

interface GitHostClient {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPR(branch: GitBranch)
  fun getOpenPRs(): List<PullRequest>
  fun closePrs(pullRequests: List<PullRequest>)

  companion object {
    enum class PrStatus {
      CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
    }

    val stub = object : GitHostClient {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPR(branch: GitBranch) {}
      override fun getOpenPRs(): List<PullRequest> = emptyList()
      override fun closePrs(pullRequests: List<PullRequest>) {}
    }
  }
}
