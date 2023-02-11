package org.virtuslab.bazelsteward.core.library

import org.virtuslab.bazelsteward.core.config.BumpingStrategy

interface Library {
  val id: LibraryId
  val version: Version
  val versioningSchema: VersioningSchema
  val bumpingStrategy: BumpingStrategy
}
