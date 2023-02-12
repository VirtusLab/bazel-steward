package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateLogic

object VersionOnlyHeuristic : Heuristic {
  override val name: String = "version-only"

  override fun apply(
    files: List<TextFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion
  ): LibraryUpdate? {
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex = Regex(Regex.escape(currentVersion))
    val matchResult = files.firstNotNullOfOrNull { regex.find(it.content)?.to(it.path) } ?: return null
    matchResult.first.next()?.let { return null }
    val versionGroup = matchResult.first.groups[0] ?: return null
    return LibraryUpdate(
      updateSuggestion, listOf(
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
