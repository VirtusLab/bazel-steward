package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.library.Version

data class BazelStewardGitBranch(val branchPrefix : String, val version: Version) {
  val prefix = branchPrefix
  val gitBranch = GitBranch(prefix + version)

  companion object {
    const val bazelPrefix = "bazel-steward"
  }
}
