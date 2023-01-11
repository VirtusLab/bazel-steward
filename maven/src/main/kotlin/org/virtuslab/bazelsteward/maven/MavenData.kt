package org.virtuslab.bazelsteward.maven

data class MavenData(
  val repositories: List<String>,
  val dependencies: List<MavenCoordinates>
)
