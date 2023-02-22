package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object PythonFunctionCallHeuristic : VersionReplacementHeuristic {
  override val name: String = "python-function-call"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val functionCalls = getFunctionCalls(files)

    if (functionCalls.isNotEmpty()) {
      val associatedStringVariants = updateSuggestion.currentLibrary.id.associatedStrings()
      val functionCallsWithAssociatedStrings = functionCalls.filter { call ->
        associatedStringVariants.any { strings ->
          strings.all { call.matchedText.contains(it) }
        }
      }

      val currentVersion = updateSuggestion.currentLibrary.version.value
      val matchedVersion = functionCallsWithAssociatedStrings.firstNotNullOfOrNull { call ->
        Regex.fromLiteral(currentVersion).find(call.matchedText)?.let { call.subMatch(it) }
      } ?: return null

      return LibraryUpdate(
        updateSuggestion,
        listOf(
          FileChange(
            matchedVersion.origin,
            matchedVersion.offset,
            updateSuggestion.currentLibrary.version.value.length,
            updateSuggestion.suggestedVersion.value
          )
        )
      )
    }
    return null
  }

  private fun getFunctionCalls(files: List<TextFile>): List<MatchedText> {
    val pythonMethodRegex = Regex("""\w+\([\w\n\s".\-,=]+\)""")
    return files.flatMap { textFile ->
      pythonMethodRegex.findAll(textFile.content).map { MatchedText(it, textFile.path) }
    }
  }
}
