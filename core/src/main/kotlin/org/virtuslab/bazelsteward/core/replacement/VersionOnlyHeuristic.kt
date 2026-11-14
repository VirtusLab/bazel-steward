package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.LibraryId

object VersionOnlyHeuristic : BaseVersionOnlyHeuristic("version-only") {
  override fun versionToRegex(currentVersion: String): Regex =
    Regex.escape(currentVersion).toRegex()
}

object VersionOnlyInStringLiteralHeuristic : BaseVersionOnlyHeuristic("version-only-in-string-literal") {
  override fun versionToRegex(currentVersion: String): Regex =
    """(?<=["'])(${Regex.escape(currentVersion)})(?=["'])""".toRegex()
}

/**
 * Matches the bare version text, without anything tying it to the library.
 * Since that is weak evidence, the match is accepted only when it is the single
 * occurrence across all searched files, and it is not an argument of a function call
 * that is about some other library (e.g. `bazel_dep(name = "rules_python", version = "2.5.1")`
 * when updating a maven artifact that is also at 2.5.1).
 */
abstract class BaseVersionOnlyHeuristic(override val name: String) : VersionReplacementHeuristic {

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val currentLibrary = updateSuggestion.currentLibrary
    val regex = versionToRegex(currentLibrary.version.value)
    val (file, match) = files.asSequence()
      .flatMap { f -> regex.findAll(f.content).map { f to MatchedText(it, f.path) } }
      .take(2)
      .toList()
      .singleOrNull() ?: return null

    val versionOffset = match.offsetLastMatchGroup ?: return null
    if (isArgumentOfUnrelatedCall(file, versionOffset, currentLibrary.id)) return null

    return LibraryUpdate(
      updateSuggestion,
      listOf(
        FileChange(
          match.origin,
          versionOffset,
          currentLibrary.version.value.length,
          updateSuggestion.suggestedVersion.value,
        ),
      ),
    )
  }

  private fun isArgumentOfUnrelatedCall(file: TextFile, offset: Int, libraryId: LibraryId): Boolean {
    val associatedStringVariants = libraryId.associatedStrings()
    return PythonFunctionCallHeuristic.getFunctionCalls(listOf(file)).any { call ->
      offset in call.match.range &&
        associatedStringVariants.none { strings -> strings.all { call.matchedText.contains(it) } }
    }
  }

  protected abstract fun versionToRegex(currentVersion: String): Regex
}
