package org.virtuslab.bazelsteward.bazel

import arrow.core.Option
import arrow.core.continuations.ensureNotNull
import arrow.core.continuations.option
import arrow.core.flattenOption
import kotlinx.coroutines.*
import org.virtuslab.bazelsteward.core.library.LibraryId
import java.nio.file.Path

class FileUpdateSearch(private val buildDefinitions: List<Pair<Path, String>>) {
  data class FileChangeSuggestion(val file: Path, val position: Int, val old: String, val new: String)

  suspend fun <Lib : LibraryId> searchBuildFiles(updateSuggestions: List<UpdateLogic.UpdateSuggestion<Lib>>): List<FileChangeSuggestion> =
    coroutineScope {
      updateSuggestions.map { suggestion ->
        async {
          findSuggestion(buildDefinitions, suggestion)
        }
      }.awaitAll().flattenOption()
    }

  private suspend fun <Lib : LibraryId> findSuggestion(
    files: List<Pair<Path, String>>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib>
  ): Option<FileChangeSuggestion> =
    option {
      val markers =
        updateSuggestion.currentLibrary.id.associatedStrings()
      val currentVersion = updateSuggestion.currentLibrary.version.value
      val regex = (markers + currentVersion)
        .map { """(${Regex.escape(it)})""" }
        .reduce { acc, s -> "$acc.*$s" }
        .let { Regex(it) }
      val matchResult = ensureNotNull(files.firstNotNullOfOrNull { regex.find(it.second)?.to(it.first) })
      val versionGroup = ensureNotNull(matchResult.first.groups[3])
      FileChangeSuggestion(
        matchResult.second,
        versionGroup.range.first,
        currentVersion,
        updateSuggestion.suggestedVersion.value
      )
    }
}
