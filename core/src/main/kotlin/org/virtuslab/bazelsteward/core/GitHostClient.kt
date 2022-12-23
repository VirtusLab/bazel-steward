package org.virtuslab.bazelsteward.core

interface GitHostClient {
  fun checkIfPrExists(branch: GitBranch): Boolean
  fun openNewPR(branch: GitBranch): Boolean

  companion object {
    val stub = object : GitHostClient {
      override fun checkIfPrExists(branch: GitBranch) = false
      override fun openNewPR(branch: GitBranch) = true
    }
  }
}
