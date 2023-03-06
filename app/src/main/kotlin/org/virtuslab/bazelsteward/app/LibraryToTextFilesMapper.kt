package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.library.Library

class LibraryToTextFilesMapper(
  private val searchPatternProvider: SearchPatternProvider,
  private val fileFinder: FileFinder
) {

  private val cache: MutableMap<Set<PathPattern>, List<TextFile>> = mutableMapOf()

  fun map(currentLibrary: Library): List<TextFile> {

    return searchPatternProvider.resolveForLibrary(currentLibrary).let {
      val pathPatternsSet = it.toSet()
      cache[pathPatternsSet]
        ?: fileFinder.find(it).also { fileList ->
          cache[pathPatternsSet] = fileList
        }
    }
  }
}
