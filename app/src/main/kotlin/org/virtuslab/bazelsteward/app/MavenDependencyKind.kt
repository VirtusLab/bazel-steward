package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Path

class MavenDependencyKind(
  private val mavenDataExtractor: MavenDataExtractor,
  private val mavenRepository: MavenRepository
) : DependencyKind<MavenLibraryId> {

  override val name: String = "maven"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<Library<MavenLibraryId>, List<Version>> {
    val data = mavenDataExtractor.extract()
    return mavenRepository.findVersions(data) as Map<Library<MavenLibraryId>, List<Version>>
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