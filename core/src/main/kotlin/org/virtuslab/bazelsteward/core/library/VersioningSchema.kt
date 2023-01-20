package org.virtuslab.bazelsteward.core.library

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import java.util.Locale

enum class VersioningType {
  SEMVER, LOOSE, REGEX;

  fun isEqualTo(name: String): Boolean = this.name == name.uppercase(Locale.getDefault())
  fun getLowercaseName():String = this.name.lowercase(Locale.getDefault())
}

class VersioningSchema(value: String) {

  val regex: String?
  var type: VersioningType

  private fun validateRegex(regexPattern: String): Option<Throwable> {
    val expectedGroups = listOf("<major>", "<minor>", "<patch>", "<preRelease>", "<buildMetaData>")
    return runCatching {
      Regex(regexPattern)
      val namedGroupsFromConfigRegex = Regex("<([a-zA-Z][a-zA-Z0-9]*)>").findAll(regexPattern).toList().map { it.value }
      if (!namedGroupsFromConfigRegex.containsAll(expectedGroups)) {
        throw Exception(
          """Regex provided in the configuration: $regexPattern 
          |does not contain all required groups: ${expectedGroups.joinToString()}""".trimMargin()
        )
      }
      None
    }.getOrElse { Option(it) }
  }

  init {
    val regexValidation = if (value.startsWith("regex:")) validateRegex(value) else null
    require(VersioningType.SEMVER.isEqualTo(value) || VersioningType.LOOSE.isEqualTo(value) || regexValidation?.isEmpty() ?: false) {
      """Versioning schema must be either a custom regex or have one of two forms: ${VersioningType.SEMVER.getLowercaseName()}, ${VersioningType.LOOSE.getLowercaseName()}.
        ${regexValidation?.getOrElse { "" } ?: ""}"""
    }
    if (regexValidation != null) {
      type = VersioningType.REGEX
      regex = value.removePrefix("regex:")
    } else {
      type = enumValueOf(value.uppercase(Locale.getDefault()))
      regex = null
    }
  }
}
