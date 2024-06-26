package org.virtuslab.bazelsteward.maven

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.PythonFunctionCallHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyInStringLiteralHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class MavenDependencyKind(
  private val mavenDataExtractor: MavenDataExtractor,
  private val mavenRepository: MavenRepository,
) : DependencyKind<MavenCoordinates>() {

  override val name: String = "maven"

  override fun acceptsLibrary(library: Library): Boolean = library is MavenCoordinates

  override suspend fun findAvailableVersions(workspaceRoot: Path, skip: (MavenCoordinates) -> Boolean): Map<MavenCoordinates, List<Version>> {
    val data = runCatching { mavenDataExtractor.extract().filterNot(skip) }
      .onFailure {
        logger.error { "Failed to extract used maven dependencies. Bazel Steward supports rules_jvm_external 4.0 or newer" }
      }.getOrThrow()
    logger.info { "Repositories " + data.repositories.toString() }
    logger.info { "Dependencies: " + data.dependencies.map { "${it.id} ${it.version}" }.toString() }
    return mavenRepository.findVersions(data)
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Glob("**/BUILD{,.bazel}"),
    PathPattern.Glob("**/*.bzl"),
    PathPattern.Exact("MODULE.bazel"),
    PathPattern.Exact("WORKSPACE.bzlmod"),
    PathPattern.Exact("WORKSPACE.bazel"),
    PathPattern.Exact("WORKSPACE"),
  )

  override val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic> = listOf(
    WholeLibraryHeuristic,
    PythonFunctionCallHeuristic,
    VersionOnlyInStringLiteralHeuristic,
  )
}
