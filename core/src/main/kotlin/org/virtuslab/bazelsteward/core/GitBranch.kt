package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class GitBranch(val name: String) {
  override fun toString(): String = name
}

data class BazelStewardGitBranch(val libraryId: LibraryId, val version: Version) {
  val prefix = "$bazelPrefix/${sanitizeLibraryId(libraryId)}/"
  val gitBranch = GitBranch(prefix + version)

  companion object {
    const val bazelPrefix = "bazel-steward"
    private fun sanitizeLibraryId(libraryId: LibraryId) = libraryId.name.replace(":", "/")
  }
}