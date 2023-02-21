package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object PythonFunctionCallHeuristic : VersionReplacementHeuristic {
  override val name: String = "python-function-call"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val pythonFunctionMatchResultList = getPythonFunctionCalls(files)

    if (pythonFunctionMatchResultList.isNotEmpty()) {

      val libAssociatedStrings = updateSuggestion.currentLibrary.id.associatedStrings()
      val libraryMatchResultList = libAssociatedStrings.flatMap {
        var listToLookFor = pythonFunctionMatchResultList
        it.flatMap { stringForRegex ->
          filterRegexInListOfMatchResults(stringForRegex, listToLookFor).also { res -> listToLookFor = res }
        }
      }

      val currentVersion = updateSuggestion.currentLibrary.version.value
      val versionMatchResultList = filterRegexInListOfMatchResults(currentVersion, libraryMatchResultList, false)
      if (versionMatchResultList.isEmpty()) return null

      val versionOffset = libraryMatchResultList.first().getRangeStart() + versionMatchResultList.first().getRangeStart()

      return LibraryUpdate(
        updateSuggestion,
        listOf(
          FileChange(
            versionMatchResultList.first().path,
            versionOffset,
            updateSuggestion.currentLibrary.version.value.length,
            updateSuggestion.suggestedVersion.value
          )
        )
      )
    }
    return null
  }

  private fun getPythonFunctionCalls(files: List<TextFile>): List<MatchResultPath> {
    val pythonMethodRegex = Regex("""\w+\([\w\n\s".\-,=]+\)""")
    return files.flatMap { textFile ->
      pythonMethodRegex.findAll(textFile.content).map { MatchResultPath(it, textFile.path) }
    }
  }

  private fun filterRegexInListOfMatchResults(
    stringForRegex: String,
    listToLookFor: List<MatchResultPath>,
    returnOriginal: Boolean = true
  ): List<MatchResultPath> =
    listToLookFor.mapNotNull {
      Regex(Regex.escape(stringForRegex)).find(it.getValue())?.let { found ->
        if (returnOriginal) it else MatchResultPath(found, it.path)
      }
    }
}
