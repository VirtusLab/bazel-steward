package org.virtuslab.bazelsteward.core.library

abstract class Version {
  abstract val value: String

  open fun toSemVer(versioning: VersioningSchema): SemanticVersion? = SemanticVersion.fromString(value, versioning)

  final override fun toString(): String = value
}
