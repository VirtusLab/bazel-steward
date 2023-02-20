package org.virtuslab.bazelsteward.config.repo

import org.virtuslab.bazelsteward.core.library.LibraryId

sealed interface DependencyNameFilter {
  fun test(libraryId: LibraryId): Boolean

  data class Default(val value: String) : DependencyNameFilter {
    override fun test(libraryId: LibraryId): Boolean {
      return if (value.contains("*")) {
        toRegex(value).matches(libraryId.name)
      } else {
        libraryId.name == value
      }
    }

    private fun toRegex(value: String): kotlin.text.Regex {
      return "((?=\\*)|(?<=\\*))".toRegex().split(value)
        .joinToString("") { if (it == "*") ".*" else kotlin.text.Regex.escape(it) }
        .toRegex()
    }
  }

  data class Regex(val value: kotlin.text.Regex) : DependencyNameFilter {
    override fun test(libraryId: LibraryId): Boolean = value.matches(libraryId.name)
  }

  companion object {
    fun parse(value: String): DependencyNameFilter {
      return when {
        value.startsWith("regex:") -> Regex(value.removePrefix("regex:").toRegex())
        else -> Default(value)
      }
    }
  }
}
