package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version

class UpdateLogic {
  data class UpdateSuggestion<Lib : LibraryId>(val currentLibrary: Library<Lib>, val suggestedVersion: Version)

  fun <Lib : LibraryId> selectUpdate(
    library: Library<Lib>,
    availableVersions: List<Version>
  ): UpdateSuggestion<Lib>? {

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): SemanticVersion? =
      availableVersions
        .mapNotNull { it.toSemVer(library.versioningSchema) }
        .filter { it.prerelease.isBlank() && filterVersionComponent(it) }
        .maxOrNull()

    return library.version.toSemVer(library.versioningSchema)
      ?.takeIf { version -> version.prerelease.isBlank() }
      ?.let { version ->
        val maxPatch = maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor }?.takeIf { it.patch > version.patch }
        val maxMinor = maxAvailableVersion { a -> a.major == version.major }?.takeIf { it.minor > version.minor }
        val maxMajor = maxAvailableVersion { _ -> true }?.takeIf { it.major > version.major }
        val nextLibrary = when (library.bumpingStrategy) {
          BumpingStrategy.Default -> maxPatch ?: maxMinor ?: maxMajor
          BumpingStrategy.Latest -> maxMajor ?: maxMinor ?: maxPatch
          BumpingStrategy.Minor -> maxMinor ?: maxPatch
        }
        nextLibrary?.let { UpdateSuggestion(library, it) }
      }
  }
}
