package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.replacement.BazelRuleHeuristic
import org.virtuslab.bazelsteward.core.replacement.Heuristic
import org.virtuslab.bazelsteward.core.rules.RuleLibrary
import org.virtuslab.bazelsteward.core.rules.RuleLibraryId
import org.virtuslab.bazelsteward.github.GithubRulesResolver
import org.virtuslab.bazelsteward.rules.BazelRulesExtractor
import java.nio.file.Path

class BazelRulesDependencyKind(
  private val bazelRulesExtractor: BazelRulesExtractor,
  private val githubRulesResolver: GithubRulesResolver
) : DependencyKind<RuleLibraryId> {
  override val name: String = "bazel-rules"

  override suspend fun findAvailableVersions(workspaceRoot: Path): Map<Library, List<Version>> {
    val usedBazelRules = bazelRulesExtractor.extractCurrentRules(workspaceRoot)
    return usedBazelRules
      .associateWith { it: RuleLibrary -> githubRulesResolver.resolveRuleVersions(it.id).values.toList() }
      as Map<Library, List<Version>>
  }

  override val defaultSearchPatterns: List<PathPattern> = listOf(
    PathPattern.Glob("**/BUILD{,.bazel}"),
    PathPattern.Glob("**/*.bzl"),
    PathPattern.Exact("WORKSPACE.bazel"),
    PathPattern.Exact("WORKSPACE")
  )

  override val defaultVersionDetectionHeuristics: List<Heuristic> = listOf(BazelRuleHeuristic)
}