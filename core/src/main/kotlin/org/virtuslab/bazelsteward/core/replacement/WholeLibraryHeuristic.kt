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
      (marker + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.let { Regex(it) }
    }
    val matchResult = regexes.map { regex ->
      files.firstNotNullOfOrNull {
        regex.find(it.content)?.to(it.path)
      } ?: return null
    }
    val versionGroup = matchResult.first().first.groups[3] ?: return null

    return LibraryUpdate(
      updateSuggestion,
      listOf(
        FileChange(
          matchResult.first().second,
          versionGroup.range.first,
          updateSuggestion.currentLibrary.version.value.length,
          updateSuggestion.suggestedVersion.value
        )
      )
    )
  }
}
