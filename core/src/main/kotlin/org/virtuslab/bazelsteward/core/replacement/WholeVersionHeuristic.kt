package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId

class WholeVersionHeuristic: Heuristic {
  override val name: String
    get() = "whole-version"

  override fun <Lib : LibraryId> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): FileUpdateSearch.FileChangeSuggestion? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      (markers + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.let { Regex(it) }
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[3] ?: return null
    return FileUpdateSearch.FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }


}