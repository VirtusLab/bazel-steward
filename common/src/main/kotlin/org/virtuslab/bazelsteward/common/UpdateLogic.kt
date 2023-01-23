package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.config.BazelStewardConfig
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
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
    return library.version.toSemVer(versioningSchemaForLibrary)
      ?.takeIf { version -> version.prerelease.isBlank() && version.buildmetadata.isBlank() }
      ?.let { version ->
        availableVersions
          .asSequence()
          .mapNotNull { it.toSemVer(versioningSchemaForLibrary) }
          .filter { it.major == version.major }
          .filter { it.buildmetadata.isBlank() && it.prerelease.isBlank() }
          .filter { version < it }
          .maxOrNull()?.let { nextVersion -> UpdateSuggestion(library, nextVersion) }
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
