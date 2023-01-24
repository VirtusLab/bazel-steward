package org.virtuslab.bazelsteward.core.library

interface Version {
  val value: String
  fun toSemVer(versioning: VersioningSchema): SemanticVersion? = SemanticVersion.fromString(value, versioning)
}
