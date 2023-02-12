package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateLogic

class FileChangeSuggester {
  fun suggestChanges(
    files: List<TextFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion,
    heuristics: List<Heuristic>
  ): LibraryUpdate? {
    return heuristics.firstNotNullOfOrNull { heuristic ->
      heuristic.apply(files, updateSuggestion)
    }
  }
}
