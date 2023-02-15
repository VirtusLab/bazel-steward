package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object WholeLibraryHeuristic : VersionReplacementHeuristic {
  override val name: String = "whole-library"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings().first()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex =
      (markers + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.let { Regex(it) }
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    val versionGroup = matchResult.first.groups[3] ?: return null
    return LibraryUpdate(
      updateSuggestion,
      listOf(
        FileChange(
          matchResult.second,
          versionGroup.range.first,
          updateSuggestion.currentLibrary.version.value.length,
          updateSuggestion.suggestedVersion.value
        )
      )
    )
  }
}
