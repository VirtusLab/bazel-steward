package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId

class VersionHeuristic : Heuristic {
  override val name: String
    get() = "version"

  override fun <Lib : LibraryId> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): FileUpdateSearch.FileChangeSuggestion? {
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      currentVersion.map { """(${Regex.escape(it.toString())})""" }.reduce { acc, s -> "$acc.*$s" }.let { Regex(it) }
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[0] ?: return null
    return FileUpdateSearch.FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }
}
