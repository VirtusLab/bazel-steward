package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import java.nio.file.Path

interface DependencyKind<Lib : Library> {
  val name: String
  suspend fun findAvailableVersions(workspaceRoot: Path): Map<Lib, List<Version>>
  val defaultSearchPatterns: List<PathPattern>
  val defaultVersionDetectionHeuristics: List<Heuristic>
}
