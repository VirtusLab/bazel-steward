package org.virtuslab.bazelsteward.core.library

import java.time.Instant

abstract class Version {
  abstract val value: String
  open val date: Instant? = null

  open fun toSemVer(versioning: VersioningSchema): SemanticVersion? = SemanticVersion.fromString(value, versioning)

  final override fun toString(): String = value
}
