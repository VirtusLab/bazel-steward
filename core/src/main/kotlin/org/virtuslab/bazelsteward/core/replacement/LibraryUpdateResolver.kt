package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

class LibraryUpdateResolver {
  fun resolve(
    files: List<TextFile>,
    updateSuggestion: UpdateSuggestion,
    heuristics: List<VersionReplacementHeuristic>,
  ): LibraryUpdate? {
    val preProcessedFiles = files.map { file -> file.map { stripComments(it) } }
    return heuristics.firstNotNullOfOrNull { heuristic ->
      heuristic.apply(preProcessedFiles, updateSuggestion)
    }
  }

  private fun stripComments(s: String): String {
    val regex = "#.*".toRegex()
    return s.lines().joinToString("\n") { line ->
      regex.replace(line) { match ->
        " ".repeat(match.value.length)
      }
    }
  }
}
