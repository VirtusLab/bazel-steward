package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object VersionOnlyHeuristic : VersionReplacementHeuristic {
  override val name: String = "version-only"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regex = Regex.escape(currentVersion).toRegex()
    val matchResult = files.firstNotNullOfOrNull { f ->
      regex.find(f.content)?.let {
        MatchedText(it, f.path)
      }
    } ?: return null
    matchResult.match.next()?.let { return null }
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
