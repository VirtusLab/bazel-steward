package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

class UpdateLogic {
  data class UpdateSuggestion<Lib : LibraryId>(val currentLibrary: Library<Lib>, val suggestedVersion: Version)

  fun <Lib : LibraryId> selectUpdate(
    library: Library<Lib>,
    availableVersions: List<Version>
  ): UpdateSuggestion<Lib>? =
    library.version.toSemVer()
      ?.takeIf { version -> version.prerelease.isBlank() && version.buildmetadata.isBlank() }
      ?.let { version ->
        availableVersions
          .asSequence()
          .mapNotNull { it.toSemVer() }
          .filter { it.major == version.major }
          .filter { it.buildmetadata.isBlank() && it.prerelease.isBlank() }
          .filter { version < it }
          .maxOrNull()?.let { nextVersion -> UpdateSuggestion(library, nextVersion) }
      }
}
