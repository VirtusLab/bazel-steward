package org.virtuslab.bazelsteward.core.library

import java.util.*

abstract class Version {
  abstract val value: String
  abstract val date: Date?

  open fun toSemVer(versioning: VersioningSchema): SemanticVersion? = SemanticVersion.fromString(value, versioning)

  final override fun toString(): String = value
}
