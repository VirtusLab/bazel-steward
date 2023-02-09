package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

interface GitHostClient {
  fun checkPrStatus(branch: GitBranch): PrStatus
  fun openNewPR(branch: GitBranch)
  fun closePrs(library: LibraryId, filterNotVersion: Version? = null)

  companion object {
    enum class PrStatus {
      CLOSED, MERGED, NONE, OPEN_MERGEABLE, OPEN_NOT_MERGEABLE, OPEN_MODIFIED
    }

    val stub = object : GitHostClient {
      override fun checkPrStatus(branch: GitBranch) = PrStatus.NONE
      override fun openNewPR(branch: GitBranch) {}
      override fun closePrs(library: LibraryId, filterNotVersion: Version?) {}
    }
  }
}
