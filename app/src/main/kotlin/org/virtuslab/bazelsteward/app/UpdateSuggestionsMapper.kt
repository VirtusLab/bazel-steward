package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

class UpdateSuggestionsMapper(
  private val searchPatternProvider: SearchPatternProvider,
  private val fileFinder: FileFinder,
  private val kindFiles: List<TextFile>
) {

  data class CacheEntry(val patterns: List<PathPattern>, val files: List<TextFile>)

  private val cache: MutableList<CacheEntry> = mutableListOf()

  fun map(updateSuggestion: UpdateSuggestion): List<TextFile> {
    val librarySearchPattern = searchPatternProvider.resolveForLibrary(updateSuggestion.currentLibrary)

    return librarySearchPattern?.let {
      findPatternInCache(it)?.files
        ?: fileFinder.find(it).let { fileList ->
          cache.add(CacheEntry(librarySearchPattern, fileList))
          fileList
        }
    } ?: kindFiles
  }

  private fun findPatternInCache(patterns: List<PathPattern>): CacheEntry? {
    return cache.firstOrNull { it.patterns equalsIgnoreOrder patterns }
  }

  private infix fun <T> List<T>.equalsIgnoreOrder(other: List<T>) =
    this.size == other.size && this.toSet() == other.toSet()
}
