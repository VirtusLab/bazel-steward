package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import java.nio.file.Path

object PythonFunctionCallHeuristic : VersionReplacementHeuristic {
  override val name: String = "python-function-call"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val pythonFunctionMatchResult = getPythonFunctionCalls(files)

    if (pythonFunctionMatchResult.isNotEmpty()) {
      val groupIndex = 0
      val artifactIndex = 1

      val libAssociatedStrings = updateSuggestion.currentLibrary.id.associatedStrings()
      val libraryMatchResult = libAssociatedStrings.flatMap {
        val groupMatchResult = findRegexInListOfMatchResults(it[groupIndex], pythonFunctionMatchResult)
        findRegexInListOfMatchResults(it[artifactIndex], groupMatchResult)
      }

      val currentVersion = updateSuggestion.currentLibrary.version.value
      val versionMatchResult = findRegexInListOfMatchResults(currentVersion, libraryMatchResult, false)
      if (versionMatchResult.isEmpty()) return null

      val versionOffset = libraryMatchResult.first().first.range.let {
        versionMatchResult.first().first.range.first.plus(it.first)
      }

      return LibraryUpdate(
        updateSuggestion,
        listOf(
          FileChange(
            versionMatchResult.first().second,
            versionOffset,
            updateSuggestion.currentLibrary.version.value.length,
            updateSuggestion.suggestedVersion.value
          )
        )
      )
    }
    return null
  }

  private fun getPythonFunctionCalls(files: List<TextFile>): List<Pair<MatchResult, Path>> {
    val pythonMethodRegex = Regex("""\w+\([\w\n\s".\-,=]+\)""")
    return files.flatMap { textFile ->
      pythonMethodRegex.findAll(textFile.content).map { it to textFile.path }
    }
  }

  private fun findRegexInListOfMatchResults(
    regexString: String,
    listToLookFor: List<Pair<MatchResult, Path>>,
    returnOriginal: Boolean = true
  ): List<Pair<MatchResult, Path>> =
    listToLookFor.mapNotNull {
      Regex(Regex.escape(regexString)).find(it.first.value)?.let { found ->
        if (returnOriginal) it else found to it.second
      }
    }
}
