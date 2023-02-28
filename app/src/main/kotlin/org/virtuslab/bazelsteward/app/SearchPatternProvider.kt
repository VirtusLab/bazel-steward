package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.SearchPatternConfig
import org.virtuslab.bazelsteward.config.repo.UpdateRulesConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library

class SearchPatternProvider(
  configs: List<SearchPatternConfig>,
  libraryConfigs: List<UpdateRulesConfig>,
  dependencyKinds: List<DependencyKind<*>>
) {

  private val applier = DependencyFilterApplier(configs, dependencyKinds)
  private val libraryApplier = DependencyFilterApplier(libraryConfigs, dependencyKinds)

  fun resolveForKind(kind: DependencyKind<*>): List<PathPattern> {
    val filter = applier.forKind(kind)
    val patternSearch = filter.findAllNotNull { kind }
    return patternSearch
      .flatMap { it.searchPattern }
      .takeIf { it.isNotEmpty() }
      ?: kind.defaultSearchPatterns
  }

  fun resolveForLibrary(library: Library): List<PathPattern>? {
    val filter = libraryApplier.forLibrary(library)
    val patternSearch = filter.findAllNotNull { library }
    return patternSearch
      .flatMap { it.searchPattern }
      .takeIf { it.isNotEmpty() }
  }
}
