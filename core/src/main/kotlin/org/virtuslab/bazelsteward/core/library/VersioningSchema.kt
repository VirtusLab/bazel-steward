package org.virtuslab.bazelsteward.core.library

sealed class VersioningSchema {
  object SemVer : VersioningSchema()
  object Loose : VersioningSchema()
  data class Regex(val regex: kotlin.text.Regex) : VersioningSchema() {
    override fun equals(other: Any?): Boolean =
      (other is Regex) && other.regex.pattern == regex.pattern && other.regex.options == regex.options // for some reason Kotlin's Regex implementation compares instances by default

    override fun hashCode(): Int = 42 * regex.pattern.hashCode() + regex.options.hashCode()
  }
}
