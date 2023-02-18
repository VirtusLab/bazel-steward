package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import java.nio.file.Path

interface DependencyKind<Lib : Library> {
  val name: String
  fun acceptsLibrary(library: Library): Boolean
  suspend fun findAvailableVersions(workspaceRoot: Path): Map<Lib, List<Version>>
  val defaultSearchPatterns: List<PathPattern>
  val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic>
}
