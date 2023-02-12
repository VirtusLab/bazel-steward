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
}