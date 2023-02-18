package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.library.Version

sealed interface PinningStrategy {
  fun test(version: Version): Boolean

  data class Prefix(val value: String) : PinningStrategy {
    override fun test(version: Version): Boolean = version.value.startsWith(this.value)
  }

  data class Exact(val value: String) : PinningStrategy {
    override fun test(version: Version): Boolean = version.value == this.value
  }

  data class Regex(val value: String) : PinningStrategy {
    override fun test(version: Version): Boolean = this.value.toRegex().matches(version.value)
  }

  object None : PinningStrategy {
    override fun test(version: Version): Boolean = true
  }

  companion object {

    fun parse(pin: String?): PinningStrategy {
      return when {
        pin == null || pin == "" -> None
        pin.startsWith("prefix:") -> Prefix(pin.removePrefix("prefix:").trim())
        pin.startsWith("regex:") -> Regex(pin.removePrefix("regex:").trim())
        pin.startsWith("exact:") -> Exact(pin.removePrefix("exact:").trim())
        "(?:\\d+\\.){1,2}".toRegex().matches(pin) -> Prefix(pin)
        "\$|()?^*{}+".any { it in pin } && runCatching { pin.toRegex() }.isSuccess -> Regex(pin)
        pin.endsWith(".") -> Prefix(pin)
        else -> Exact(pin)
      }
    }
  }
}
