package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.config.BazelStewardConfig
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.core.library.VersioningType
import org.virtuslab.bazelsteward.maven.MavenLibraryId

class UpdateLogic(private val bazelStewardConfig: BazelStewardConfig) {
  data class UpdateSuggestion<Lib : LibraryId>(val currentLibrary: Library<Lib>, val suggestedVersion: Version)

  fun <Lib : LibraryId> selectUpdate(
    library: Library<Lib>,
    availableVersions: List<Version>
  ): UpdateSuggestion<Lib>? {
    val versioningSchemaForLibrary = getVersioningForLibrary(library)

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): SemanticVersion? =
      availableVersions
        .mapNotNull { it.toSemVer(versioningSchemaForLibrary) }
        .filter { it.prerelease.isBlank() && filterVersionComponent(it) }
        .sorted()
        .maxOrNull()

    return library.version.toSemVer(versioningSchemaForLibrary)
      ?.takeIf { version -> version.prerelease.isBlank() }
      ?.let { version ->
        val maxPatchVersion = maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor }
        val maxMinorVersion = maxAvailableVersion { a -> a.major == version.major }
        val maxMajorVersion = maxAvailableVersion { _ -> true }
        val nextLibrary = maxPatchVersion?.takeIf { it.patch > version.patch }
          ?: maxMinorVersion?.takeIf { it.minor > version.minor }
          ?: maxMajorVersion?.takeIf { it.major > version.major }
        nextLibrary?.let { UpdateSuggestion(library, it) }
      }
  }

  private fun <Lib : LibraryId> getVersioningForLibrary(library: Library<Lib>): VersioningSchema {
    return when (val libraryId = library.id) {
      is MavenLibraryId -> {
        val matchingDependencies = bazelStewardConfig.maven.ruledDependencies.filter { it.id == libraryId }
        matchingDependencies.firstOrNull()?.versioning ?: VersioningSchema(VersioningType.LOOSE.name)
      }
      else -> VersioningSchema(VersioningType.LOOSE.name)
    }
  }
}
