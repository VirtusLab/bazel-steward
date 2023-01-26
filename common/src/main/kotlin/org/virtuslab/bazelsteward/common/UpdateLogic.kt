package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.config.BazelStewardConfig
import org.virtuslab.bazelsteward.config.BumpingStrategy
import org.virtuslab.bazelsteward.config.ConfigEntry
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
    val (versioningSchema, bumpingStrategy) = getConfigurableSetupForLibrary(library)

    fun maxAvailableVersion(filterVersionComponent: (a: SemanticVersion) -> Boolean): SemanticVersion? =
      availableVersions
        .mapNotNull { it.toSemVer(versioningSchema) }
        .filter { it.prerelease.isBlank() && filterVersionComponent(it) }
        .maxOrNull()

    return library.version.toSemVer(versioningSchema)
      ?.takeIf { version -> version.prerelease.isBlank() }
      ?.let { version ->
        val maxPatch = maxAvailableVersion { a -> a.major == version.major && a.minor == version.minor }?.takeIf { it.patch > version.patch }
        val maxMinor = maxAvailableVersion { a -> a.major == version.major }?.takeIf { it.minor > version.minor }
        val maxMajor = maxAvailableVersion { _ -> true }?.takeIf { it.major > version.major }
        val nextLibrary = when (bumpingStrategy) {
          BumpingStrategy.DEFAULT -> maxPatch ?: maxMinor ?: maxMajor
          BumpingStrategy.LATEST -> maxMajor ?: maxMinor ?: maxPatch
        }
        nextLibrary?.let { UpdateSuggestion(library, it) }
      }
  }

  private fun getConfigEntryFromConfigs(libraryId: MavenLibraryId, configs: List<ConfigEntry>): ConfigEntry? =
    configs.firstOrNull { it.group == libraryId.group && it.artifact == libraryId.artifact }
      ?: configs.firstOrNull { it.group == libraryId.group && it.artifact == null }
      ?: configs.firstOrNull { it.group == null && it.artifact == null }

  private fun <Lib : LibraryId> getConfigurableSetupForLibrary(library: Library<Lib>): Pair<VersioningSchema, BumpingStrategy> {
    return when (val libraryId = library.id) {
      is MavenLibraryId -> {
        val versioningForDependency = getConfigEntryFromConfigs(libraryId, bazelStewardConfig.maven.configs.filter { it.versioning != null })
        val bumpingForDependency = getConfigEntryFromConfigs(libraryId, bazelStewardConfig.maven.configs.filter { it.bumping != null })
        Pair(versioningForDependency?.versioning ?: VersioningSchema(VersioningType.LOOSE.name), bumpingForDependency?.bumping ?: BumpingStrategy.DEFAULT)
      }
      else -> Pair(VersioningSchema(VersioningType.LOOSE.name), BumpingStrategy.DEFAULT)
    }
  }
}
