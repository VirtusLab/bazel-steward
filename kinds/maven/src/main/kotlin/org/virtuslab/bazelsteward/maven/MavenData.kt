package org.virtuslab.bazelsteward.maven

data class MavenData(
  val repositories: List<String>,
  val dependencies: List<MavenCoordinates>,
) {
  fun filterNot(skip: (MavenCoordinates) -> Boolean): MavenData {
    return this.copy(dependencies = dependencies.filterNot(skip))
  }
}
