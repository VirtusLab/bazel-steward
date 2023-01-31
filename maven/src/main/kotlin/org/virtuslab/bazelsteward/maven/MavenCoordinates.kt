package org.virtuslab.bazelsteward.maven

import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class MavenLibraryId(val group: String, val artifact: String) : LibraryId {
  override fun associatedStrings(): List<String> = listOf(group, artifact)

  override val name: String
    get() = artifact
}

data class MavenCoordinates(
  override val id: MavenLibraryId,
  override val version: Version,
  override val versioningSchema: VersioningSchema,
  override val bumpingStrategy: BumpingStrategy,
) : Library<MavenLibraryId> {
  companion object {
    fun of(
      group: String,
      artifact: String,
      version: String,
      versioningSchema: VersioningSchema = VersioningSchema.Loose,
      bumpingStrategy: BumpingStrategy = BumpingStrategy.Default
    ): MavenCoordinates = MavenCoordinates(MavenLibraryId(group, artifact), SimpleVersion(version), versioningSchema, bumpingStrategy)
  }
}
