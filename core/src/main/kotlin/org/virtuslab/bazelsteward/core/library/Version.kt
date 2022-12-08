package org.virtuslab.bazelsteward.core.library

import arrow.core.Option

interface Version {
  val value: String
  fun toSemVer(): Option<SemanticVersion> = SemanticVersion.fromString(value)
}
