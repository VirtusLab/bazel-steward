package org.virtuslab.bazelsteward.e2e

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient

abstract class CountingGitHostClient : GitHostClient {
  val openNewPrCalls: MutableList<GitBranch> = ArrayList(10)
  val closeOldPrsCalls: MutableList<GitBranch> = ArrayList(10)
  override fun openNewPR(branch: GitBranch) {
    openNewPrCalls.add(branch)
  }

  override fun closeOldPrs(newBranch: GitBranch) {
    closeOldPrsCalls.add(newBranch)
  }
}
