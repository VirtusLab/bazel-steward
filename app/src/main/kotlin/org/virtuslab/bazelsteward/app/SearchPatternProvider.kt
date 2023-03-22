package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.config.repo.SearchPatternConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library

class SearchPatternProvider(
  configs: List<SearchPatternConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {

  private val logger = KotlinLogging.logger {}
  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveForLibrary(library: Library): List<PathPattern> {
    val filter = applier.forLibrary(library)
    return filter.findNotNull { it.pathPatterns }?.pathPatterns
      ?: filter.kind?.defaultSearchPatterns
      ?: emptyList<PathPattern>().also { logger.warn { "No search patterns for $library" } }
  }
}
