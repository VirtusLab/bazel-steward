package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId

interface Heuristic {
  val name: String
  fun <Lib : LibraryId> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): FileUpdateSearch.FileChangeSuggestion?
}
