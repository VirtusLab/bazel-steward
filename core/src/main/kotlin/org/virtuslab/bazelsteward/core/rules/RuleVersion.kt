package org.virtuslab.bazelsteward.core.rules

import org.virtuslab.bazelsteward.core.library.Version

data class RuleVersion(val url: String, val sha256: String, val tag: String) : Version {
  override val value: String
    get() = tag
}
