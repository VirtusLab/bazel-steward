package org.virtuslab.bazelsteward.bzlmod

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class BazelModuleId(override val name: String) : LibraryId() {
  override fun associatedStrings(): List<List<String>> = listOf(listOf(name))
}

data class BazelModule(
  override val id: BazelModuleId,
  override val version: Version,
) : Library
