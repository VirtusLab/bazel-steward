package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import java.nio.file.Path

data class LibraryUpdate(
  val suggestion: UpdateSuggestion,
  val fileChanges: List<FileChange>
)

data class MatchResultPath(
  val matchResult: MatchResult,
  val path: Path
) {
  fun getRangeStart(): Int = matchResult.range.start
  fun getGroup(index: Int): MatchGroup? = matchResult.groups[index]

  fun getValue(): String = matchResult.value
}

interface VersionReplacementHeuristic {
  val name: String
  fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate?
}
