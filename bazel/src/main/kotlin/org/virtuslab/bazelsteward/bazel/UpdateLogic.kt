package org.virtuslab.bazelsteward.bazel

import arrow.core.Option
import arrow.core.continuations.ensureNotNull
import arrow.core.continuations.option
import arrow.core.flattenOption
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

class UpdateLogic {
  data class UpdateSuggestion<Lib : LibraryId>(val currentLibrary: Library<Lib>, val suggestedVersion: Version)

  suspend fun <Lib : LibraryId> selectUpdate(
    library: Library<Lib>,
    availableVersions: List<Version>
  ): Option<UpdateSuggestion<Lib>> =
    option {
      val version = library.version.toSemVer().bind()
      ensure(version.prerelease.isBlank())
      ensure(version.buildmetadata.isBlank())
      val nextVersion = ensureNotNull(
        availableVersions
          .map { it.toSemVer() }
          .flattenOption()
          .filter { it.major == version.major }
          .filter { it.buildmetadata.isBlank() && it.prerelease.isBlank() }
          .filter { version < it }
          .maxOrNull()
      )
      UpdateSuggestion(library, nextVersion)
    }
}
