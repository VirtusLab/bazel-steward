package org.virtuslab.bazelsteward.core

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.common.TextFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

class FileFinder(private val workspaceRoot: Path) {

  fun find(patterns: List<PathPattern>): List<TextFile> {
    val javaMatchers = patterns.filterIsInstance<PathPattern.JavaPathMatcher>()
    val exactMatchers = patterns.filterIsInstance<PathPattern.Exact>()

    val matchedByPattern = if (javaMatchers.isNotEmpty()) {
      Files.walk(workspaceRoot)
        .filter { path -> javaMatchers.any { it.matcher.matches(path) } }
        .collect(Collectors.toList())
    } else {
      emptyList<Path>()
    }

    val matchedExactly = exactMatchers.map { workspaceRoot.resolve(it.value) }.filter { it.exists() }

    val allPaths = matchedExactly + matchedByPattern

    logger.debug { "Found paths: ${allPaths.joinToString("\n", "\n", "\n")}" }

    return allPaths.map(TextFile::from)
  }
}
