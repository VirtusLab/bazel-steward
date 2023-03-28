package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class BazelStewardGitBranch(val libraryId: LibraryId, val version: Version) {
  val prefix = "$bazelPrefix/${sanitizeLibraryId(libraryId)}/"
  val gitBranch = GitBranch(prefix + version)

  companion object {
    const val bazelPrefix = "bazel-steward:update"
    private fun sanitizeLibraryId(libraryId: LibraryId) = libraryId.name.replace(":", "/")
  }
}
