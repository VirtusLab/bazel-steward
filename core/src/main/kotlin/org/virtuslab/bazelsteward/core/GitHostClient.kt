package org.virtuslab.bazelsteward.core

interface GitHostClient {
  fun checkRP(branch: GitBranch)
  fun openPR(branch: GitBranch)
}