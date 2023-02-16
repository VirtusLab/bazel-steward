package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.library.Version

sealed class PinningStrategy {
  abstract val value: String
  abstract fun test(version: Version): Boolean

  data class Prefix(override val value: String) : PinningStrategy() {
    override fun test(version: Version): Boolean = version.value.startsWith(this.value)
  }

  data class Exact(override val value: String) : PinningStrategy() {
    override fun test(version: Version): Boolean = version.value == this.value
  }

  data class Regex(override val value: String) : PinningStrategy() {
    override fun test(version: Version): Boolean = this.value.toRegex().matches(version.value)
  }

  companion object {

    fun parse(pin: String?): PinningStrategy? {
      return when {
        pin == null || pin == "" -> null
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
