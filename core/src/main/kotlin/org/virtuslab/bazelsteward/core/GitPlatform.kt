package org.virtuslab.bazelsteward.core

data class PullRequest(val branch: GitBranch)

data class NewPullRequest(
  val branch: GitBranch,
  val title: String,
  val body: String,
  val labels: List<String>,
)

interface GitPlatform {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPr(pr: NewPullRequest): PullRequest
  fun getOpenPrs(): List<PullRequest>
  fun closePrs(pullRequests: List<PullRequest>)
  suspend fun onPrChange(pr: PullRequest, prStatusBefore: PrStatus)

  enum class PrStatus {
    CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
  }

  companion object {
    val stub = object : GitPlatform {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPr(pr: NewPullRequest) = PullRequest(pr.branch)
      override fun getOpenPrs(): List<PullRequest> = emptyList()
      override fun closePrs(pullRequests: List<PullRequest>) {}
      override suspend fun onPrChange(pr: PullRequest, prStatusBefore: PrStatus) {}
    }
  }
}
