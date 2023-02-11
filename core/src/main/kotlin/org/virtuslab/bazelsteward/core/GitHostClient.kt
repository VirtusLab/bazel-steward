package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class PullRequest(val branch: GitBranch)

interface GitHostClient {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPR(branch: GitBranch)
  fun closePrs(library: LibraryId, filterNotVersion: Version? = null)
  fun getOpenPRs(): List<PullRequest>
  fun closePrs(pullRequest: List<PullRequest>)

  companion object {
    enum class PrStatus {
      CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
    }

    val stub = object : GitHostClient {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPR(branch: GitBranch) {}
      override fun closePrs(library: LibraryId, filterNotVersion: Version?) {}
      override fun getOpenPRs(): List<PullRequest> = emptyList()
      override fun closePrs(pullRequest: List<PullRequest>) {}
    }
  }
}
