package org.virtuslab.bazelsteward.config.repo

interface DependencyFilter {
  val kinds: List<String>
  val dependencies: List<DependencyNameFilter>
}
