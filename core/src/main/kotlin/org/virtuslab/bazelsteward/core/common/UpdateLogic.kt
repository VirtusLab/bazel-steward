package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class UpdateSuggestion(val currentLibrary: Library, val suggestedVersion: Version)

data class UpdateRules(
  val versioningSchema: VersioningSchema = VersioningSchema.Loose,
  val bumpingStrategy: BumpingStrategy = BumpingStrategy.MinorPatchMajor,
  val pinningStrategy: PinningStrategy = PinningStrategy.None,
  val enabled: Boolean = true,
)

class UpdateLogic {

  fun selectUpdate(
    library: Library,
    availableVersions: List<Version>,
    updateRules: UpdateRules,
  ): UpdateSuggestion? {
    fun checkPreRelease(): List<Pair<Version, SemanticVersion>> =
      availableVersions
        .filter { version -> updateRules.pinningStrategy.test(version) }
        .mapNotNull { version -> version.toSemVer(updateRules.versioningSchema)?.let { version to it } }
        .filter { it.second.prerelease.isBlank() }

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): Version? =
      checkPreRelease()
        .filter { filterVersionComponent(it.second) }
        .maxByOrNull { it.second }
        ?.first

    fun selectDefault(): UpdateSuggestion? =
      library.version.toSemVer(updateRules.versioningSchema)
        ?.takeIf { version -> version.prerelease.isBlank() }
        ?.let { version ->
          val maxPatch =
            maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor && a.patch > version.patch }
          val maxMinor = maxAvailableVersion { a -> a.major == version.major && a.minor > version.minor }
          val maxMajor = maxAvailableVersion { a -> a.major > version.major }
          val nextVersion = when (updateRules.bumpingStrategy) {
            BumpingStrategy.Minimal -> maxPatch ?: maxMinor ?: maxMajor
            BumpingStrategy.Latest -> maxMajor ?: maxMinor ?: maxPatch
            BumpingStrategy.MinorPatchMajor -> maxMinor ?: maxPatch ?: maxMajor
            BumpingStrategy.PatchOnly -> maxPatch
            BumpingStrategy.PatchMinor -> maxPatch ?: maxMinor
            BumpingStrategy.MinorPatch -> maxMinor ?: maxPatch
            else -> null
          }
          nextVersion?.let { UpdateSuggestion(library, it) }
        }

    fun maxAvailableVersionByDate(): Version? =
      checkPreRelease()
        .map { it.first }
        .filter { it.date != null }
        .maxByOrNull { it.date!! }

    fun selectByDate(): UpdateSuggestion? {
      return library.version.toSemVer(updateRules.versioningSchema)
        ?.takeIf { version -> version.prerelease.isBlank() }
        ?.let {
          maxAvailableVersionByDate()?.let {
            if (it.value != library.version.value) {
              UpdateSuggestion(library, it)
            } else {
              null
            }
          }
        }
    }

    if (!updateRules.enabled) return null

    return if (updateRules.bumpingStrategy == BumpingStrategy.LatestByDate) {
      selectByDate()
    } else {
      selectDefault()
    }
  }
}
