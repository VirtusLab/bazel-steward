package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.app.provider.SearchPatternProvider
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.TextFileResolver
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.library.Library

class DefaultTextFileResolver(
  private val searchPatternProvider: SearchPatternProvider,
  private val fileFinder: FileFinder,
) : TextFileResolver {

  private val cache: MutableMap<Set<PathPattern>, List<TextFile>> = mutableMapOf()

  override fun resolve(library: Library): List<TextFile> {
    val pathPatterns = searchPatternProvider.resolveForLibrary(library)
    val cacheKey = pathPatterns.toSet()
    return cache.getOrPut(cacheKey) { fileFinder.find(pathPatterns) }
  }
}
