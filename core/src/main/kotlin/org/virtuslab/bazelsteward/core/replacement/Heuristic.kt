package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

data class LibraryUpdate<Lib : LibraryId, V : Version>(
  val updateSuggestion: UpdateLogic.UpdateSuggestion<Lib, V>,
  val fileChanges: List<FileChange>
)

interface Heuristic {
  val name: String
  fun <Lib : LibraryId, V : Version> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib, V>
  ): LibraryUpdate<Lib, V>?
}
