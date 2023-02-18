package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

data class LibraryUpdate(
  val suggestion: UpdateSuggestion,
  val fileChanges: List<FileChange>
)

interface VersionReplacementHeuristic {
  val name: String
  fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate?
}
