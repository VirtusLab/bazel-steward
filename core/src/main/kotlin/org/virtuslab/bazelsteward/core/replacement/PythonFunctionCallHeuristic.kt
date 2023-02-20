package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import java.nio.file.Path

object PythonFunctionCallHeuristic : VersionReplacementHeuristic {
  override val name: String = "python-function-call"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val pythonFunctionMatchResult = getPythonFunctions(files)

    if (pythonFunctionMatchResult.isNotEmpty()) {
      val libraryMatchResult = updateSuggestion.currentLibrary.id.associatedStrings().let { libAssociatedStrings ->
        (libAssociatedStrings.size - 1).let { associatedIndex ->
          findRegexInListOfMatchResults(
            libAssociatedStrings[associatedIndex][1],
            findRegexInListOfMatchResults(libAssociatedStrings[associatedIndex][0], pythonFunctionMatchResult)
          )
        }
      }

      val currentVersion = updateSuggestion.currentLibrary.version.value
      val versionMatchResult = findRegexInListOfMatchResults(currentVersion, libraryMatchResult, false)
      if (versionMatchResult.isEmpty()) return null

      val versionOffset = libraryMatchResult.first()?.first?.range?.let {
        versionMatchResult.first()?.first?.range?.first?.plus(it.first) ?: return null
      } ?: return null

      return LibraryUpdate(
        updateSuggestion,
        listOf(
          FileChange(
            versionMatchResult.first()!!.second,
            versionOffset,
            updateSuggestion.currentLibrary.version.value.length,
            updateSuggestion.suggestedVersion.value
          )
        )
      )
    }
    return null
  }

  private fun getPythonFunctions(files: List<TextFile>): List<Pair<MatchResult, Path>?> {
    val pythonMethodRegex = Regex("\\w+.\\w+\\([a-zA-Z0-1\\n\\s\".,-=]+\\)")
    return files
      .map { textFile -> pythonMethodRegex.findAll(textFile.content).map { it to textFile.path }.toList() }
      .flatten()
  }

  private fun findRegexInListOfMatchResults(
    regexString: String,
    listToLookFor: List<Pair<MatchResult, Path>?>,
    returnOriginal: Boolean = true
  ): List<Pair<MatchResult, Path>?> =
    listToLookFor.mapNotNull {
      it?.let {
        Regex(Regex.escape(regexString)).find(it.first.value)?.let { found ->
          if (returnOriginal) it else found to it.second
        }
      }
    }
}
