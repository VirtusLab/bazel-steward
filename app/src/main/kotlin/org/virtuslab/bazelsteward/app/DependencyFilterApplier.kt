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
    val kind = dependencyKinds.find { it.acceptsLibrary(library) }
    val configsForKinds = configs.filter { conf -> conf.kinds.isEmpty() || (kind != null && conf.kinds.contains(kind.name)) }
    return FilteredByKind(configsForKinds, library.id, kind)
  }

  class FilteredByKind<T : DependencyFilter>(
    private val configs: List<T>,
    private val libraryId: LibraryId,
    val kind: DependencyKind<*>?
  ) {
    private fun find(predicate: (T) -> Boolean): T? {
      val filteredConfigs = configs.filter { predicate(it) }
      return filteredConfigs.firstOrNull { it.dependencies.any { f -> f.test(libraryId) } }
        ?: kind?.let { filteredConfigs.firstOrNull { it.kinds.any { f -> f == kind.name } } }
        ?: filteredConfigs.firstOrNull { it.dependencies.isEmpty() }
    }

    fun findNotNull(getter: (T) -> Any?): T? {
      return find { getter(it) != null }
    }
  }
}
