package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.library.Version

sealed class PinningStrategy(val value: String) {
  data class Prefix(val prefix: String) : PinningStrategy(prefix)

  data class Regex(val regex: String) : PinningStrategy(regex)

  data class Exact(val exact: String) : PinningStrategy(exact)

  fun test(version: Version): Boolean {
    return when (this) {
      is Prefix -> version.value.startsWith(this.value)
      is Exact -> version.value == this.value
      is Regex -> kotlin.text.Regex(this.value).matches(version.value)
    }
  }

  companion object {

    fun create(pin: String): PinningStrategy? {
      return when {
        pin.startsWith("prefix") -> Prefix(pin)
        pin.startsWith("regex") -> Regex(pin)
        pin.startsWith("exact") -> Exact(pin)
        pin.endsWith(".") -> Prefix(pin)
        !pin.endsWith(".") -> Exact(pin)
        else -> null
      }
    }
  }
}
