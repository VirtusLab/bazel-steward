package org.virtuslab.bazelsteward.core.library

import org.virtuslab.bazelsteward.core.config.BumpingStrategy

interface Library {
  val id: LibraryId
  val version: Version
  val versioningSchema: VersioningSchema
  val bumpingStrategy: BumpingStrategy
  fun withVersioningSchema(schema: VersioningSchema): Library
  fun withBumpingStrategy(strategy: BumpingStrategy): Library
}
