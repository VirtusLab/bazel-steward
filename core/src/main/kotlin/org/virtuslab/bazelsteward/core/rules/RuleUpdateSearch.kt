package org.virtuslab.bazelsteward.core.rules

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import java.nio.file.Path

object RuleUpdateSearch {
  data class FileChangeSuggestion(
    val library: Library<LibraryId>,
    val file: Path,
    val patches: List<Replacement>,
    val version: Version,
  ) {
    data class Replacement(val position: Int, val lengthToReplace: Int, val patch: String)
  }

  fun <Lib : RuleLibraryId> searchBuildFiles(
    buildDefinitions: List<BazelFileSearch.BazelFile>,
    updateSuggestions: List<UpdateLogic.UpdateSuggestion<Lib, RuleVersion>>
  ): List<FileChangeSuggestion> = updateSuggestions.flatMap { suggestion -> findSuggestion(buildDefinitions, suggestion) }

  private fun <Lib : RuleLibraryId> findSuggestion(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib, RuleVersion>
  ): List<FileChangeSuggestion> {
    val currentUrl = updateSuggestion.currentLibrary.id.downloadUrl
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val currentSha = updateSuggestion.currentLibrary.id.sha256

    return with(updateSuggestion.suggestedVersion) {
      listOf(currentUrl, currentVersion, currentSha).zip(listOf(url, value, sha256)).flatMap { (current, suggested) ->
        val regex = """(${Regex.escape(current)})""".toRegex()
        files
          .map { regex.findAll(it.content) to it.path }
          .map { result -> result.first.singleOrNull()?.groups?.first()?.range?.let { Change(it.first, it.last - it.first + 1, suggested, result.second) } }
      }
    }.filterNotNull()
      .groupBy { it.filePath }
      .map { (file, changes) -> FileChangeSuggestion(updateSuggestion.currentLibrary, file, changes.map { FileChangeSuggestion.Replacement(it.position, it.length, it.change) }, updateSuggestion.suggestedVersion) }
  }

  private data class Change(val position: Int, val length: Int, val change: String, val filePath: Path)
}
