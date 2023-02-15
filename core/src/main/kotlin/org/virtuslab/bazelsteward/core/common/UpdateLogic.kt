package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class UpdateSuggestion(val currentLibrary: Library, val suggestedVersion: Version)

data class UpdateRules(
  val versioningSchema: VersioningSchema = VersioningSchema.Loose,
  val bumpingStrategy: BumpingStrategy = BumpingStrategy.Default,
  val pinVersion: PinningStrategy? = null
)

class UpdateLogic {

  fun selectUpdate(
    library: Library,
    availableVersions: List<Version>,
    updateRules: UpdateRules
  ): UpdateSuggestion? {

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): Version? =
      availableVersions
        .filter { version -> updateRules.pinVersion?.test(version) ?: true }
        .mapNotNull { version -> version.toSemVer(updateRules.versioningSchema)?.let { version to it } }
        .filter { it.second.prerelease.isBlank() && filterVersionComponent(it.second) }
        .maxByOrNull { it.second }
        ?.first

    return library.version.toSemVer(updateRules.versioningSchema)
      ?.takeIf { version -> version.prerelease.isBlank() }
      ?.let { version ->
        val maxPatch =
          maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor && a.patch > version.patch }
        val maxMinor = maxAvailableVersion { a -> a.major == version.major && a.minor > version.minor }
        val maxMajor = maxAvailableVersion { a -> a.major > version.major }
        val nextVersion = when (updateRules.bumpingStrategy) {
          BumpingStrategy.Default -> maxPatch ?: maxMinor ?: maxMajor
          BumpingStrategy.Latest -> maxMajor ?: maxMinor ?: maxPatch
          BumpingStrategy.Minor -> maxMinor ?: maxPatch
        }
        nextVersion?.let { UpdateSuggestion(library, it) }
      }
  }
}
