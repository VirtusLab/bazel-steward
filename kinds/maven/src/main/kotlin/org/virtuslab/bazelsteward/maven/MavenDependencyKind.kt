package org.virtuslab.bazelsteward.maven

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class MavenDependencyKind(
  private val mavenDataExtractor: MavenDataExtractor,
  private val mavenRepository: MavenRepository
) : DependencyKind<MavenCoordinates> {

  override val name: String = "maven"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<MavenCoordinates, List<Version>> {
    val data = mavenDataExtractor.extract()
    logger.debug { "Repositories " + data.repositories.toString() }
    logger.debug { "Dependencies: " + data.dependencies.map { it.id.name + " " + it.version.value }.toString() }
    return mavenRepository.findVersions(data)
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Glob("**/BUILD{,.bazel}"),
    PathPattern.Glob("**/*.bzl"),
    PathPattern.Exact("WORKSPACE.bazel"),
    PathPattern.Exact("WORKSPACE")
  )

  override val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic> = listOf(
    WholeLibraryHeuristic,
    VersionOnlyHeuristic
  )
}
