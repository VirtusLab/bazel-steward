package org.virtuslab.bazelsteward.bzlmod

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.PythonFunctionCallHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class BzlModDependencyKind(
  private val bzlModDataExtractor: BzlModDataExtractor,
  private val bzlModRepository: BzlModRepository,
) : DependencyKind<BazelModule>() {

  override val name: String = "bzlmod"

  override fun acceptsLibrary(library: Library): Boolean = library is BazelModule

  override suspend fun findAvailableVersions(workspaceRoot: Path, skip: (BazelModule) -> Boolean): Map<BazelModule, List<Version>> {
    val data = bzlModDataExtractor.extract().filterNot(skip)
    logger.info { "Repositories " + data.repositories.toString() }
    logger.info { "Dependencies: " + data.dependencies.map { "${it.id} ${it.version}" }.toString() }
    return bzlModRepository.findVersions(data)
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Exact("MODULE.bazel"),
    PathPattern.Exact("MODULE"),
  )

  override val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic> = listOf(
    PythonFunctionCallHeuristic,
    WholeLibraryHeuristic,
    VersionOnlyHeuristic,
  )
}
