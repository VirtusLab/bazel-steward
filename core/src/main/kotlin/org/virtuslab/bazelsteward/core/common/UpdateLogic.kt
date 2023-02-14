package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class UpdateSuggestion(val currentLibrary: Library, val suggestedVersion: Version)

data class UpdateData(val versioningSchema: VersioningSchema = VersioningSchema.Loose,
                       val bumpingStrategy: BumpingStrategy = BumpingStrategy.Default,
                       val pinVersion: Version? = null)

class UpdateLogic {

  fun selectUpdate(
    library: Library,
    availableVersions: List<Version>,
    updateData: UpdateData
  ): UpdateSuggestion? {

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): Version? =
      availableVersions
        .filter{ version -> updateData.pinVersion?.let{ version.value.startsWith(it.value) } ?: true }
        .mapNotNull { version -> version.toSemVer(updateData.versioningSchema)?.let { version to it } }
        .filter { it.second.prerelease.isBlank() && filterVersionComponent(it.second) }
        .maxByOrNull { it.second }
        ?.first

    return library.version.toSemVer(updateData.versioningSchema)
      ?.takeIf { version -> version.prerelease.isBlank() }
      ?.let { version ->
        val maxPatch =
          maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor && a.patch > version.patch }
        val maxMinor = maxAvailableVersion { a -> a.major == version.major && a.minor > version.minor }
        val maxMajor = maxAvailableVersion { a -> a.major > version.major }
        val nextVersion = when (updateData.bumpingStrategy) {
          BumpingStrategy.Default -> maxPatch ?: maxMinor ?: maxMajor
          BumpingStrategy.Latest -> maxMajor ?: maxMinor ?: maxPatch
          BumpingStrategy.Minor -> maxMinor ?: maxPatch
        }
        nextVersion?.let { UpdateSuggestion(library, it) }
      }
  }
}
