package org.virtuslab.bazelsteward.core

data class PullRequest(val branch: GitBranch)

data class NewPullRequest(
  val branch: GitBranch,
  val title: String,
  val body: String,
  val labels: List<String>
)

interface GitHostClient {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPR(pr: NewPullRequest)
  fun getOpenPRs(): List<PullRequest>
  fun closePrs(pullRequests: List<PullRequest>)

  enum class PrStatus {
    CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
  }

  companion object {
    val stub = object : GitHostClient {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPR(pr: NewPullRequest) {}
      override fun getOpenPRs(): List<PullRequest> = emptyList()
      override fun closePrs(pullRequests: List<PullRequest>) {}
    }
  }
}
