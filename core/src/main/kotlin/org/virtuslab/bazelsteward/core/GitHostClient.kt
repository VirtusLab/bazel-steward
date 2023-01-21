package org.virtuslab.bazelsteward.core

interface GitHostClient {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPR(branch: GitBranch)
  fun closeOldPrs(newBranch: GitBranch)

  companion object {
    enum class PrStatus {
      CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
    }

    val stub = object : GitHostClient {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPR(branch: GitBranch) {}
      override fun closeOldPrs(newBranch: GitBranch) {}
    }
  }
}
