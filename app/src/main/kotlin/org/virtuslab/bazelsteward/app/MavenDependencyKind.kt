package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class MavenDependencyKind(
  private val mavenDataExtractor: MavenDataExtractor,
  private val mavenRepository: MavenRepository
) : DependencyKind<MavenLibraryId> {

  override val name: String = "maven"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<Library, List<Version>> {
    val data = mavenDataExtractor.extract()
    logger.debug { "Repositories " + data.repositories.toString() }
    logger.debug { "Dependencies: " + data.dependencies.map { it.id.name + " " + it.version.value }.toString() }
    return mavenRepository.findVersions(data) as Map<Library, List<Version>>
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Glob("**/BUILD{,.bazel}"),
    PathPattern.Glob("**/*.bzl"),
    PathPattern.Exact("WORKSPACE.bazel"),
    PathPattern.Exact("WORKSPACE")
  )

  override val defaultVersionDetectionHeuristics: List<Heuristic> = listOf(
    WholeLibraryHeuristic,
    VersionOnlyHeuristic
  )
}