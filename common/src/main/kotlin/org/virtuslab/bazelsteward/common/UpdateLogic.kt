package org.virtuslab.bazelsteward.common

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

  private fun <Lib : LibraryId> getVersioningForLibrary(library: Library<Lib>): VersioningSchema {
    return when (val libraryId = library.id) {
      is MavenLibraryId -> {
        val matchingDependencies = bazelStewardConfig.maven.ruledDependencies.filter { it.id == libraryId }
        when {
          matchingDependencies.isEmpty() -> VersioningSchema(VersioningType.LOOSE.name)
          matchingDependencies.size == 1 -> matchingDependencies.first().versioning
          else -> throw Exception(
            "In the configuration file more than one ruled dependency is declared for " +
              "group: ${libraryId.group} and artifact: ${libraryId.artifact}"
          )
        }
      }
      else -> VersioningSchema(VersioningType.LOOSE.name)
    }
  }
}
