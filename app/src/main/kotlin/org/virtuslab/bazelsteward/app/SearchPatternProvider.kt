package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.SearchPatternConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library

class SearchPatternProvider(
  configs: List<SearchPatternConfig>,
  private val dependencyKinds: List<DependencyKind<*>>
) {

  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  private fun resolveForKind(kind: DependencyKind<*>): List<PathPattern> {
    val filter = applier.forKind(kind)
    val patternSearch = filter.findAllNotNull { kind }
    return patternSearch
      .flatMap { it.pathPatterns }
      .takeIf { it.isNotEmpty() }
      ?: kind.defaultSearchPatterns
  }

  fun resolveForLibrary(library: Library): List<PathPattern> {
    val filter = applier.forLibrary(library)
    val patternSearch = filter.findAllNotNull { library }
    return patternSearch
      .flatMap { it.pathPatterns }
      .takeIf { it.isNotEmpty() }
      ?: resolveForKind(dependencyKinds.first { it.acceptsLibrary(library) })
  }
}
