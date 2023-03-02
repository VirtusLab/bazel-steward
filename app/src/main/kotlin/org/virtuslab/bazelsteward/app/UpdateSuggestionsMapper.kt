package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

class UpdateSuggestionsMapper(
  private val searchPatternProvider: SearchPatternProvider,
  private val fileFinder: FileFinder
) {

  private val cache: MutableMap<Set<PathPattern>, List<TextFile>> = mutableMapOf()

  fun map(updateSuggestion: UpdateSuggestion): List<TextFile> {
    val librarySearchPattern = searchPatternProvider.resolveForLibrary(updateSuggestion.currentLibrary)

    return librarySearchPattern.let {
      cache[it.toSet()]
        ?: fileFinder.find(it).also { fileList ->
          cache[librarySearchPattern.toSet()] = fileList
        }
    }
  }
}
