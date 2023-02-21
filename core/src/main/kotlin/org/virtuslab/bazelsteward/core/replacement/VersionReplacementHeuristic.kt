package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import java.nio.file.Path

data class LibraryUpdate(
  val suggestion: UpdateSuggestion,
  val fileChanges: List<FileChange>
)

data class MatchedText(
  val match: MatchResult,
  val origin: Path
) {
  val offset: Int
    get() = match.range.start

  val offsetLastMatchGroup: Int?
    get() = match.groups.last()?.range?.start
  val matchedText: String
    get() = match.value

  fun subMatch(other: MatchResult): Pair<MatchedText, MatchedText> = this to MatchedText(other, origin)
}

interface VersionReplacementHeuristic {
  val name: String
  fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate?
}
