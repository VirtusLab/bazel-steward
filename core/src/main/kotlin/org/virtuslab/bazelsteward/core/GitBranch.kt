package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class GitBranch(val libraryId: LibraryId, val version: Version) {
  val name = libraryId.groupName?.let {
    "$branchPrefix/${libraryId.groupName}/${libraryId.name}/${version.value}"
  } ?: "$branchPrefix/${libraryId.name}/${version.value}"

  companion object {
    const val branchPrefix = "bazel-steward"
  }
}
