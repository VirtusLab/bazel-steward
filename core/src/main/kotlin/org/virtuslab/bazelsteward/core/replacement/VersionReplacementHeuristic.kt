package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import java.nio.file.Path

data class LibraryUpdate(
  val suggestion: UpdateSuggestion,
  val fileChanges: List<FileChange>,
)

data class MatchedText(
  val match: MatchResult,
  val origin: Path,
  val baseOffset: Int = 0,
) {
  val offset: Int
    get() = match.range.start + baseOffset

  val offsetLastMatchGroup: Int?
    get() = match.groups.last()?.range?.start
  val matchedText: String
    get() = match.value

  fun subMatch(subMatch: MatchResult): MatchedText = MatchedText(subMatch, origin, baseOffset = offset)
}

interface VersionReplacementHeuristic {
  val name: String
  fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate?
}
