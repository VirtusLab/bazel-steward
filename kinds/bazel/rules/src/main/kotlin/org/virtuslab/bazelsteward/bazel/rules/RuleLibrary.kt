package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.library.Library

data class RuleLibrary(
  override val id: RuleLibraryId,
  override val version: RuleVersion,
) : Library
