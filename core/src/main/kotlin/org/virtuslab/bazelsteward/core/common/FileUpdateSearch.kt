package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.nio.file.Path

data class FileChangeSuggestion(
  val library: Library,
  val newVersion: Version,
  val file: Path,
  val position: Int
) {
  val branch = GitBranch("${library.id} $newVersion")
}

class FileUpdateSearch {

  fun searchBuildFiles(
    buildDefinitions: List<BazelFileSearch.BazelFile>,
    updateSuggestions: List<UpdateLogic.UpdateSuggestion>
  ): List<FileChangeSuggestion> =
    updateSuggestions.mapNotNull { suggestion -> findSuggestion(buildDefinitions, suggestion) }

  private fun findSuggestion(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion
  ): FileChangeSuggestion? {
    val allHeuristics = listOf(
      WholeLibraryHeuristic,
      VersionOnlyHeuristic,
    )
    return null
  }

  fun searchBazelVersionFiles(
    buildDefinitions: List<BazelFileSearch.BazelFile>,
    updateSuggestions: List<UpdateLogic.UpdateSuggestion>
  ): List<FileChangeSuggestion> =
    updateSuggestions.mapNotNull { suggestion -> findBazelSuggestion(buildDefinitions, suggestion) }

  private fun findBazelSuggestion(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion
  ): FileChangeSuggestion? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      (markers.map { """(?:${Regex.escape(it)})""" } + "(${Regex.escape(currentVersion)})").reduce { acc, s -> "$acc*.*$s" }.toRegex()
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[1] ?: return null
    return FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }
}
