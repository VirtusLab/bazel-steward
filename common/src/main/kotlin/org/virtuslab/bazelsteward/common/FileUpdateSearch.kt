package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import java.nio.file.Path

class FileUpdateSearch {
  data class FileChangeSuggestion(
    val library: Library<LibraryId>,
    val newVersion: Version,
    val file: Path,
    val position: Int
  )

  fun <Lib : LibraryId> searchBuildFiles(
    buildDefinitions: List<BazelFileSearch.BazelFile>,
    updateSuggestions: List<UpdateLogic.UpdateSuggestion<Lib>>
  ): List<FileChangeSuggestion> =
    updateSuggestions.mapNotNull { suggestion -> findSuggestion(buildDefinitions, suggestion) }

  private fun <Lib : LibraryId> findSuggestion(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): FileChangeSuggestion? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      (markers + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.let { Regex(it) }
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[3] ?: return null
    return FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }

  fun <Lib : LibraryId> searchBazelVersionFiles(
    buildDefinitions: List<BazelFileSearch.BazelFile>,
    updateSuggestions: List<UpdateLogic.UpdateSuggestion<Lib>>
  ): List<FileChangeSuggestion> =
    updateSuggestions.mapNotNull { suggestion -> findBazelSuggestion(buildDefinitions, suggestion) }

  private fun <Lib : LibraryId> findBazelSuggestion(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): FileChangeSuggestion? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      (markers.map { """(?:${Regex.escape(it)})""" } + "(${Regex.escape(currentVersion)})").reduce { acc, s -> "$acc*.*$s" }.let(::Regex)
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[1] ?: return null
    return FileChangeSuggestion(
      updateSuggestion.currentLibrary, updateSuggestion.suggestedVersion, matchResult.second, versionGroup.range.first
    )
  }
}
