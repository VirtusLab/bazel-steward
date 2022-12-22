package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class GitBranch(val libraryId: LibraryId, val version: Version) {
  val name = "bazel-steward/${libraryId.name}/${version.value}"
}
