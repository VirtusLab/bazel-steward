package org.virtuslab.bazelsteward.app

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.virtuslab.bazelsteward.bazel.rules.RuleLibrary
import org.virtuslab.bazelsteward.bazel.rules.RuleLibraryId
import org.virtuslab.bazelsteward.bazel.rules.RuleVersion
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.fixture.DependencyKindsFixture
import org.virtuslab.bazelsteward.fixture.loadRepoConfigFromResources
import org.virtuslab.bazelsteward.maven.MavenCoordinates

class SearchPatternProviderTest {

  private val config = loadRepoConfigFromResources("example-config.yaml")
  private val dependencyKinds = DependencyKindsFixture()
  private val searchPatternProvider = SearchPatternProvider(config.searchPaths, dependencyKinds.all)

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class ResolveForLibraryTest {
    @Test
    fun `should return expected searchPattern for Library`() {
      val library = MavenCoordinates.of("org.virtuslab", "dep-a", "1.0.0")
      val result = searchPatternProvider.resolveForLibrary(library)

      result shouldContainExactlyInAnyOrder listOf(
        PathPattern.Regex(""".*\/WORKSPACE[.\w]*"""),
      )
    }

    @Test
    fun `should return default for Library without custom searchPattern`() {
      val libraryIdURL = "https://github.com/aaa/aaa/archive/aaa.zip"
      val library = RuleLibrary(RuleLibraryId.from(libraryIdURL), RuleVersion.create(libraryIdURL, "aaa", "1.0.0"))

      val result = searchPatternProvider.resolveForLibrary(library)

      result shouldContainExactlyInAnyOrder listOf(
        PathPattern.Glob("**/BUILD{,.bazel}"),
        PathPattern.Glob("**/*.bzl"),
        PathPattern.Exact("WORKSPACE.bazel"),
        PathPattern.Exact("WORKSPACE"),
      )
    }
  }
}
