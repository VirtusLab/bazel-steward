package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.DependencyFilter
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId

class DependencyFilterApplier<T : DependencyFilter>(
  private val configs: List<T>,
  private val dependencyKinds: List<DependencyKind<*>>,
) {

  fun forLibrary(library: Library): FilteredByKind<T> {
    val kind = dependencyKinds.find { it.acceptsLibrary(library) }
    val configsForKinds = configs.filter { conf -> conf.kinds.isEmpty() || (kind != null && conf.kinds.contains(kind.name)) }
    return FilteredByKind(configsForKinds, library.id, kind)
  }

  fun forPredicate(predicate: (T) -> Boolean): FilteredByPredicate<T> {
    return FilteredByPredicate(configs, predicate)
  }

  interface Filtered<T : DependencyFilter> {
    fun find(predicate: (T) -> Boolean): T?

    fun <R> findNotNullOrDefault(default: R, getter: (T) -> R?): R {
      return find { getter(it) != null }?.let(getter) ?: default
    }
  }

  class FilteredByPredicate<T : DependencyFilter>(
    private val configs: List<T>,
    private val basePredicate: (T) -> Boolean,
  ) : Filtered<T> {
    override fun find(predicate: (T) -> Boolean): T? {
      val filteredConfigs = configs.filter { predicate(it) }
      return filteredConfigs.firstOrNull { basePredicate(it) }
        ?: filteredConfigs.firstOrNull { it.dependencies.isEmpty() }
    }
  }

  class FilteredByKind<T : DependencyFilter>(
    private val configs: List<T>,
    private val libraryId: LibraryId,
    val kind: DependencyKind<*>?,
  ) : Filtered<T> {
    override fun find(predicate: (T) -> Boolean): T? {
      val filteredConfigs = configs.filter { predicate(it) }
      return filteredConfigs.firstOrNull { it.dependencies.any { depFilter -> depFilter.test(libraryId) } }
        ?: filteredConfigs.firstOrNull { it.dependencies.isEmpty() }
    }
  }
}
