package org.virtuslab.bazelsteward.core.rules

import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class RuleLibrary(
  override val id: RuleLibraryId,
  override val version: Version,
  override val versioningSchema: VersioningSchema = VersioningSchema.Loose,
  override val bumpingStrategy: BumpingStrategy = BumpingStrategy.Default,
) : Library {

  override fun withVersioningSchema(schema: VersioningSchema): Library {
    return copy(versioningSchema = schema)
  }

  override fun withBumpingStrategy(strategy: BumpingStrategy): Library {
    return copy(bumpingStrategy = strategy)
  }
}
