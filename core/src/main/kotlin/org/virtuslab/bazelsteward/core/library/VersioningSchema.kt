package org.virtuslab.bazelsteward.core.library

import java.util.Locale

enum class VersioningType {
  SEMVER, LOOSE, REGEX;

  fun isEqualTo(name: String): Boolean = this.name == name.uppercase(Locale.getDefault())
  fun getLowercaseName(): String = this.name.lowercase(Locale.getDefault())
}

class VersioningSchema(value: String) {

  val regex: String?
  val type: VersioningType

  private fun validateRegex(regexPattern: String): Throwable? {
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
      null
    }.getOrElse { it }
  }

  init {
    val (regexValidation, isRegex) = if (value.startsWith("regex:")) Pair(validateRegex(value), true) else Pair(null, false)
    require(VersioningType.SEMVER.isEqualTo(value) || VersioningType.LOOSE.isEqualTo(value) || (regexValidation == null && isRegex)) {
      """Versioning schema must be either a custom regex or have one of two forms: ${VersioningType.SEMVER.getLowercaseName()}, ${VersioningType.LOOSE.getLowercaseName()}.
        |${regexValidation?.message ?: ""}""".trimMargin()
    }
    if (isRegex) {
      type = VersioningType.REGEX
      regex = value.removePrefix("regex:")
    } else {
      type = enumValueOf(value.uppercase(Locale.getDefault()))
      regex = null
    }
  }
}
