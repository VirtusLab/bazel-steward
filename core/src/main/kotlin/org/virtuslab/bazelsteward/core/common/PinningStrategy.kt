package org.virtuslab.bazelsteward.core.common

sealed class PinningStrategy(val value: String) {
  data class Prefix(val prefix: String) : PinningStrategy(prefix)

  data class Regex(val regex: String) : PinningStrategy(regex)

  data class Exact(val exact: String) : PinningStrategy(exact)
}
