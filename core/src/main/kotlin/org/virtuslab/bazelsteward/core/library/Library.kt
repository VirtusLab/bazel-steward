package org.virtuslab.bazelsteward.core.library

import org.virtuslab.bazelsteward.core.config.BumpingStrategy

interface Library<out Id : LibraryId> {
  val id: Id
  val version: Version
  val versioningSchema: VersioningSchema
  val bumpingStrategy: BumpingStrategy
}
