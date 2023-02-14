package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.common.UpdateData
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.config.ConfigEntry
import org.virtuslab.bazelsteward.core.config.RepoConfig
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.maven.MavenLibraryId

class UpdateDataCreator {

  companion object {
    fun getConfigurableSetupForLibrary(library: Library, repoConfig: RepoConfig): UpdateData {
      return when (val libraryId = library.id) {
        is MavenLibraryId -> {
          val versioningForDependency = getConfigEntryFromConfigs(libraryId, repoConfig.maven.configs.filter { it.versioning != null })
          val bumpingForDependency = getConfigEntryFromConfigs(libraryId, repoConfig.maven.configs.filter { it.bumping != null })
          val pinForDependency = getConfigEntryFromConfigs(libraryId, repoConfig.maven.configs.filter { it.pin != null })
          UpdateData(
            versioningForDependency?.versioning ?: VersioningSchema.Loose,
            bumpingForDependency?.bumping ?: BumpingStrategy.Default,
            pinForDependency?.pin?.let { SimpleVersion(it) }
          )
        }
        else -> UpdateData(VersioningSchema.Loose, BumpingStrategy.Minor)
      }
    }

    private fun getConfigEntryFromConfigs(libraryId: MavenLibraryId, configs: List<ConfigEntry>): ConfigEntry? =
      configs.firstOrNull { it.group == libraryId.group && it.artifact == libraryId.artifact }
        ?: configs.firstOrNull { it.group == libraryId.group && it.artifact == null }
        ?: configs.firstOrNull { it.group == null && it.artifact == null }
  }
}
