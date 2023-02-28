package org.virtuslab.bazelsteward.app

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Paths

class SearchPatternProviderTest {

  private fun createDependencyKinds(): List<DependencyKind<*>> {
    val workspaceRoot = Paths.get(".")
    return listOf(
      BazelVersionDependencyKind(BazelUpdater()),
      MavenDependencyKind(MavenDataExtractor(workspaceRoot), MavenRepository()),
      BazelRulesDependencyKind(BazelRulesExtractor(workspaceRoot), GithubRulesResolver(GitHub.connectAnonymously()))
    )
  }

  private val config = RepoConfigParser().parse(this::class.java.classLoader.getResource("example-config.yaml")!!.readText())
  private val dependencyKinds = createDependencyKinds()
  private val searchPatternProvider = SearchPatternProvider(config.searchPaths, config.updateRules, dependencyKinds)
  private val expectedListResult = listOf<PathPattern>(
    PathPattern.Regex(""".*\/WORKSPACE[.\w]*"""),
    PathPattern.Glob("**/*.bzl")
  )

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class ResolveForKindTest {
    @Test
    fun `should return expectedListResult for MavenDependencyKind`() {
      val kind = dependencyKinds[1]
      val result = searchPatternProvider.resolveForKind(kind)

      result shouldContainExactlyInAnyOrder expectedListResult
    }

    @Test
    fun `should return expectedListResult with additional PathPattern for BazelVersionDependencyKind`() {
      val kind = dependencyKinds[0]
      val result = searchPatternProvider.resolveForKind(kind)

      val expectedResult = mutableListOf<PathPattern>(PathPattern.Regex(""".*\/BUILD[,\.bazel]*"""))
      expectedResult.addAll(expectedListResult)

      result.shouldContainExactlyInAnyOrder(expectedResult)
    }

    @Test
    fun `should return default searchPattern for BazelRulesDependencyKind`() {
      val searchPatternProvider = SearchPatternProvider(listOf(), listOf(), dependencyKinds)
      val kind = dependencyKinds[2]
      val result = searchPatternProvider.resolveForKind(kind)

      result shouldContainExactlyInAnyOrder kind.defaultSearchPatterns
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class ResolveForLibraryTest {
    @Test
    fun `should return expected searchPattern for Library`() {
      val library = MavenCoordinates.of("org.virtuslab", "dep-a", "1.0.0")
      val result = searchPatternProvider.resolveForLibrary(library)

      result shouldContainExactlyInAnyOrder listOf(PathPattern.Regex(""".*\/WORKSPACE[.\w]*"""))
    }

    @Test
    fun `should return null for Library without custom searchPattern`() {
      val library = MavenCoordinates.of("org.virtuslab", "dep-xxx", "1.0.0")
      val result = searchPatternProvider.resolveForLibrary(library)

      result shouldBe null
    }
  }
}
