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
    val matchResult = regexes.firstNotNullOfOrNull { regex ->
      files.firstNotNullOfOrNull { textFile ->
        regex.findAll(textFile.content)
          .map { MatchedText(it, textFile.path) }
          .sortedBy { it.matchedText.length }
          .firstOrNull()
      }
    } ?: return null
    val versionOffset = matchResult.offsetLastMatchGroup ?: return null

    return LibraryUpdate(
      updateSuggestion,
      listOf(
        FileChange(
          matchResult.origin,
          versionOffset,
          updateSuggestion.currentLibrary.version.value.length,
          updateSuggestion.suggestedVersion.value,
        ),
      ),
    )
  }
}
