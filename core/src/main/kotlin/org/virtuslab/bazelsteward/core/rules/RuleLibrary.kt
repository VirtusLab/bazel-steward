package org.virtuslab.bazelsteward.core.rules

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version

data class RuleLibrary(
  override val id: RuleLibraryId,
  override val version: Version,
) : Library
