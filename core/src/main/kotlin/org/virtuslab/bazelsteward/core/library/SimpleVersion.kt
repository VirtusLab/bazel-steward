package org.virtuslab.bazelsteward.core.library

import java.util.*

data class SimpleVersion(override val value: String) : Version() {
  override val date: Date?
    get() = null
}
