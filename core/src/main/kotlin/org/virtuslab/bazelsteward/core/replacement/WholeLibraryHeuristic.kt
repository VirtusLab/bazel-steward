package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object WholeLibraryHeuristic : VersionReplacementHeuristic {
  override val name: String = "whole-library"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regexes = markers.map { marker ->
      (marker + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.toRegex()
    }
    val matchResultsList = regexes.mapNotNull { regex ->
      files.firstNotNullOfOrNull { textFile ->
        regex.find(textFile.content)?.let {
          MatchResultPath(it, textFile.path)
        }
      }
    }
    if (matchResultsList.isEmpty()) return null
    val versionGroup = matchResultsList.first().getGroup(3) ?: return null

    return LibraryUpdate(
      updateSuggestion,
      listOf(
        FileChange(
          matchResultsList.first().path,
          versionGroup.range.start,
          updateSuggestion.currentLibrary.version.value.length,
          updateSuggestion.suggestedVersion.value
        )
      )
    )
  }
}
