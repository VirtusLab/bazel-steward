package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version

data class RuleLibrary(
  override val id: RuleLibraryId,
  override val version: Version,
) : Library
