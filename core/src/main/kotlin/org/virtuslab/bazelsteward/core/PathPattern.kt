package org.virtuslab.bazelsteward.core

import java.nio.file.FileSystems
import java.nio.file.PathMatcher

sealed interface PathPattern {
  sealed class JavaPathMatcher(pattern: String, prefix: String) : PathPattern {
    val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher(prefix + pattern)
  }

  val value: String

  data class Glob(override val value: String) : JavaPathMatcher(value, "glob:")
  data class Regex(override val value: String) : JavaPathMatcher(value, "regex:")
  data class Exact(override val value: String) : PathPattern

  companion object {

    fun parse(pattern: String?): PathPattern {
      return when {
        pattern == null || pattern == "" -> throw Exception("Wrong search-pattern")
        pattern.startsWith("glob:") -> Glob(pattern.removePrefix("glob:").trim())
        pattern.startsWith("regex:") -> Regex(pattern.removePrefix("regex:").trim())
        pattern.startsWith("exact:") -> Exact(pattern.removePrefix("exact:").trim())
        "{}*,".any { it in pattern } && runCatching { Glob(pattern) }.isSuccess -> Glob(pattern)
        "\$|()?^*{}+".any { it in pattern } && runCatching { pattern.toRegex() }.isSuccess -> Regex(pattern)
        else -> Exact(pattern)
      }
    }
  }
}
