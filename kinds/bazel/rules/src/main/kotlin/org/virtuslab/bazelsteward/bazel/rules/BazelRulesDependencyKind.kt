package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import java.nio.file.Path

class BazelRulesDependencyKind(
  private val bazelRulesExtractor: BazelRulesExtractor,
  private val rulesResolver: RulesResolver,
) : DependencyKind<RuleLibrary> {
  override val name: String = "bazel-rules"

  override fun acceptsLibrary(library: Library): Boolean = library is RuleLibrary

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<RuleLibrary, List<Version>> {
    val usedBazelRules = bazelRulesExtractor.extractCurrentRules(workspaceRoot)
    return usedBazelRules
      .associateWith { it: RuleLibrary -> rulesResolver.resolveRuleVersions(it.id).values.toList() }
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Glob("**/BUILD{,.bazel}"),
    PathPattern.Glob("**/*.bzl"),
    PathPattern.Exact("WORKSPACE.bazel"),
    PathPattern.Exact("WORKSPACE"),
  )

  override val defaultVersionReplacementHeuristics: List<VersionReplacementHeuristic> = listOf(BazelRuleHeuristic)
}
