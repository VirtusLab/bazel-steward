package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.DependencyFilter
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId

class DependencyFilterApplier<T : DependencyFilter>(
  private val configs: List<T>,
  private val dependencyKinds: List<DependencyKind<*>>
) {

  fun forLibrary(library: Library): FilteredByKind<T> {
    val kind = dependencyKinds.find { it.acceptsLibrary(library) }?.name
    val configsForKinds = configs.filter { conf -> conf.kinds.isEmpty() || (kind != null && conf.kinds.contains(kind)) }
    return FilteredByKind(configsForKinds, library.id)
  }

  class FilteredByKind<T : DependencyFilter>(private val configs: List<T>, private val libraryId: LibraryId) {
    fun find(predicate: (T) -> Boolean): T? {
      val filteredConfigs = configs.filter { predicate(it) }
      return filteredConfigs.firstOrNull { it.dependencies.any { f -> f.test(libraryId) } }
        ?: filteredConfigs.firstOrNull { it.dependencies.isEmpty() }
    }

    fun findNotNull(getter: (T) -> Any?): T? {
      return find { getter(it) != null }
    }
  }
}
