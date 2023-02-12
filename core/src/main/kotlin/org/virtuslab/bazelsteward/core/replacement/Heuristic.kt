package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateLogic

data class LibraryUpdate(
  val updateSuggestion: UpdateLogic.UpdateSuggestion,
  val fileChanges: List<FileChange>
)

interface Heuristic {
  val name: String
  fun apply(
    files: List<TextFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion
  ): LibraryUpdate?
}
