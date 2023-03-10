package org.virtuslab.bazelsteward.app

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.bazel.rules.RuleLibrary
import org.virtuslab.bazelsteward.bazel.rules.RuleLibraryId.ReleaseArtifact
import org.virtuslab.bazelsteward.bazel.version.BazelLibrary
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.testing.DependencyKindsFixture
import org.virtuslab.bazelsteward.testing.loadRepoConfigFromResources
import org.virtuslab.bazelsteward.testing.prepareLocalWorkspace
import java.nio.file.Path

class LibraryToTextFilesMapperTest {

  @Test
  fun `should return correct paths for MavenDependencyKind`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir)
    val library = MavenCoordinates.of("org.virtuslab", "dep-a", "1.0.0")

    val result = testForDependencyKind(library, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      workspace.resolve("WORKSPACE")
    )
  }

  @Test
  fun `should return correct paths for BazelVersionDependencyKind`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir)
    val library = BazelLibrary(SimpleVersion("5.3.0"))

    val result = testForDependencyKind(library, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      workspace.resolve("app/BUILD"),
      workspace.resolve("core/BUILD")
    )
  }

  @Test
  fun `should return correct paths for BazelRulesDependencyKind`(@TempDir tempDir: Path) {
    val workspace = prepareWorkspace(tempDir)
    val library = RuleLibrary(
      ReleaseArtifact("", "", "", "", ""),
      SimpleVersion("5.3.0")
    )

    val result = testForDependencyKind(library, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      workspace.resolve("app/BUILD"),
      workspace.resolve("core/BUILD")
    )
  }

  private fun prepareWorkspace(tempDir: Path): Path =
    prepareLocalWorkspace(javaClass, tempDir, "config")

  private fun testForDependencyKind(
    library: Library,
    tempDir: Path
  ): List<TextFile> {
    val dependencyKinds = DependencyKindsFixture(tempDir)
    val fileFinder = FileFinder(tempDir)
    val repoConfig = loadRepoConfigFromResources(javaClass, "example-config.yaml")
    val searchPatternProvider = SearchPatternProvider(repoConfig.searchPaths, dependencyKinds.all)

    val libraryToTextFilesMapper = LibraryToTextFilesMapper(
      searchPatternProvider,
      fileFinder
    )
    return libraryToTextFilesMapper.map(library)
  }
}