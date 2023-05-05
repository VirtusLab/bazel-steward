package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.common.UpdateRules
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import java.nio.file.Path

abstract class DependencyKind<Lib : Library> {
  abstract val name: String
  abstract fun acceptsLibrary(library: Library): Boolean
  abstract suspend fun findAvailableVersions(workspaceRoot: Path, skip: (Lib) -> Boolean): Map<Lib, List<Version>>
  abstract val defaultSearchPatterns: List<PathPattern>
  abstract val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic>
  open val defaultUpdateRules: UpdateRules = UpdateRules()
}
