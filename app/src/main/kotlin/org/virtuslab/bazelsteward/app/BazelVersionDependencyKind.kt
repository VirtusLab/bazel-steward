package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.bazel.BazelUpdater
import org.virtuslab.bazelsteward.bazel.BazelVersion
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import java.lang.RuntimeException
import java.nio.file.Path

class BazelVersionDependencyKind(
  private val bazelUpdater: BazelUpdater
) : DependencyKind<BazelUpdater.BazelLibraryId> {

  override val name: String = "bazel"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<Library<BazelUpdater.BazelLibraryId>, List<Version>> {
    val version = BazelVersion.extractBazelVersion(workspaceRoot)
      ?: throw RuntimeException("Could not find bazel version")
    val library = BazelUpdater.BazelLibrary(version)
    val versions = bazelUpdater.availableVersions(version)
    return mapOf(library to versions)
  }

  override val defaultSearchPatterns: List<PathPattern> =
    listOf(".bazelversion", ".bazeliskrc").map(PathPattern::Exact)

  override val defaultVersionDetectionHeuristics: List<Heuristic> =
    listOf(WholeLibraryHeuristic, VersionOnlyHeuristic)

}
