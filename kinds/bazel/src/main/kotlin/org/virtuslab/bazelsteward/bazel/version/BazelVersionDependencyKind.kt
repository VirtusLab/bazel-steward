package org.virtuslab.bazelsteward.bazel.version

import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.lang.RuntimeException
import java.nio.file.Path

class BazelVersionDependencyKind(
  private val bazelUpdater: BazelUpdater
) : DependencyKind<BazelLibrary> {

  override val name: String = "bazel"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<BazelLibrary, List<Version>> {
    val version = BazelVersion.extractBazelVersion(workspaceRoot)
      ?: throw RuntimeException("Could not find bazel version")
    val library = BazelLibrary(version)
    val versions = bazelUpdater.availableVersions(version)
    return mapOf(library to versions)
  }

  override val defaultSearchPatterns: List<PathPattern> =
    listOf(BazelVersion.DOT_BAZEL_VERSION, BazelVersion.DOT_BAZELISK_RC).map(PathPattern::Exact)

  override val defaultVersionDetectionHeuristics: List<Heuristic> =
    listOf(WholeLibraryHeuristic, VersionOnlyHeuristic)
}
