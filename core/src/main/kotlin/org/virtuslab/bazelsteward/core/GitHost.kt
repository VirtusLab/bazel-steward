package org.virtuslab.bazelsteward.core

interface GitHost {
  fun checkRP(branch: GitBranch)
  fun openPR(branch: GitBranch)
}