package org.virtuslab.bazelsteward.fixture

import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModDataExtractor
import org.virtuslab.bazelsteward.bzlmod.BzlModDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModRepository
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Path
import java.nio.file.Paths

class DependencyKindsFixture(workspaceRoot: Path = Paths.get(".")) {

  val bazelVersion = BazelVersionDependencyKind(BazelUpdater())
  val maven = MavenDependencyKind(MavenDataExtractor(workspaceRoot, "maven"), MavenRepository())
  val bzlmod = BzlModDependencyKind(BzlModDataExtractor(workspaceRoot), BzlModRepository())
  val bazelRules = BazelRulesDependencyKind(
    BazelRulesExtractor(),
    GithubRulesResolver(GitHub.connectAnonymously()),
  )
  val all = listOf(maven, bazelVersion, bzlmod, bazelRules)
}
