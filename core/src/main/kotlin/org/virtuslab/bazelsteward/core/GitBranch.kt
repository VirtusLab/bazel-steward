package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class GitBranch(val libraryId: LibraryId, val version: Version) {
  val libraryPrefix = "$bazelPrefix/${libraryId.name}"
  val name = "$libraryPrefix/${version.value}"

  companion object {
    const val bazelPrefix = "bazel-steward"
  }
}
