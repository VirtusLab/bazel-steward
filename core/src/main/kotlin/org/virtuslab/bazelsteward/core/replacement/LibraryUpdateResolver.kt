package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

class LibraryUpdateResolver {
  fun resolve(
    files: List<TextFile>,
    updateSuggestion: UpdateSuggestion,
    heuristics: List<VersionReplacementHeuristic>,
  ): LibraryUpdate? {
    return heuristics.firstNotNullOfOrNull { heuristic ->
      heuristic.apply(files, updateSuggestion)
    }
  }
}
