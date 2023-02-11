package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

class VersionOnlyHeuristic : Heuristic {
  override val name: String = "version-only"

  override fun <Lib : LibraryId, V : Version> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib, V>
  ): FileUpdateSearch.FileChangeSuggestion? {
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex = Regex(Regex.escape(currentVersion))
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    matchResult.first.next()?.let { return null }
    val versionGroup = matchResult.first.groups[0] ?: return null
    return FileUpdateSearch.FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }
}
