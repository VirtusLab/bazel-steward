package org.virtuslab.bazelsteward.core.library

interface Version {
  val value: String
  fun toSemVer(): SemanticVersion? = SemanticVersion.fromString(value)
}
