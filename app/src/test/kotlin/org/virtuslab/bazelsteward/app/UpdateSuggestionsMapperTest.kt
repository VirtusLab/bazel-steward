package org.virtuslab.bazelsteward.app

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.rules.RuleLibrary
import org.virtuslab.bazelsteward.bazel.rules.RuleLibraryId.ReleaseArtifact
import org.virtuslab.bazelsteward.bazel.version.BazelLibrary
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Path

class UpdateSuggestionsMapperTest {

  @Test
  fun `should return correct paths for MavenDependencyKind`(@TempDir tempDir: Path) {
    createFileTreeInTempDir(tempDir)
    val library = MavenCoordinates.of("org.virtuslab", "dep-a", "1.0.0")
    val suggestedVersion = SimpleVersion("1.2.0")
    val updateSuggestion = UpdateSuggestion(library, suggestedVersion)

    val dependencyKinds = createDependencyKinds(tempDir)
    val result = testForDependencyKind(updateSuggestion, dependencyKinds[1], dependencyKinds, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      tempDir.resolve("app/resource/WORKSPACE"),
      tempDir.resolve("core/resource/WORKSPACE.bzl")
    )
  }

  @Test
  fun `should return correct paths for BazelVersionDependencyKind`(@TempDir tempDir: Path) {
    createFileTreeInTempDir(tempDir)
    val library = BazelLibrary(SimpleVersion("5.3.0"))
    val suggestedVersion = SimpleVersion("5.4.0")
    val updateSuggestion = UpdateSuggestion(library, suggestedVersion)

    val dependencyKinds = createDependencyKinds(tempDir)
    val result = testForDependencyKind(updateSuggestion, dependencyKinds[0], dependencyKinds, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      tempDir.resolve("app/resource/WORKSPACE"),
      tempDir.resolve("core/resource/WORKSPACE.bzl"),
      tempDir.resolve("core/resource/BUILD")
    )
  }

  @Test
  fun `should return correct paths for BazelRulesDependencyKind`(@TempDir tempDir: Path) {
    createFileTreeInTempDir(tempDir)
    val library = RuleLibrary(
      ReleaseArtifact("", "", "", "", ""),
      SimpleVersion("5.3.0")
    )
    val suggestedVersion = SimpleVersion("5.4.0")
    val updateSuggestion = UpdateSuggestion(library, suggestedVersion)

    val dependencyKinds = createDependencyKinds(tempDir)
    val result = testForDependencyKind(updateSuggestion, dependencyKinds[2], dependencyKinds, tempDir)

    result.map { it.path } shouldContainExactlyInAnyOrder listOf(
      tempDir.resolve("core/resource/BUILD")
    )
  }

  private fun createFileTreeInTempDir(tempDir: Path) {
    FileUtils.forceMkdir(tempDir.resolve("app/resource").toFile())
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource("WORKSPACE.bzlignore"),
      tempDir.resolve("app/resource/WORKSPACE").toFile()
    )
    FileUtils.forceMkdir(tempDir.resolve("core/resource").toFile())
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource("WORKSPACE.bzlignore"),
      tempDir.resolve("core/resource/WORKSPACE.bzl").toFile()
    )
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource("example-config.yaml"),
      tempDir.resolve("core/resource/BUILD").toFile()
    )
  }

  private fun createDependencyKinds(tempDir: Path): List<DependencyKind<*>> {
    return listOf(
      BazelVersionDependencyKind(BazelUpdater()),
      MavenDependencyKind(MavenDataExtractor(tempDir), MavenRepository()),
      BazelRulesDependencyKind(BazelRulesExtractor(tempDir), GithubRulesResolver(GitHub.connectAnonymously()))
    )
  }
  
  private fun testForDependencyKind(
    updateSuggestions: UpdateSuggestion,
    kind: DependencyKind<*>,
    dependencyKinds: List<DependencyKind<*>>,
    tempDir: Path
  ): List<TextFile> {
    val fileFinder = FileFinder(tempDir)
    val repoConfig = RepoConfigParser().parse(this::class.java.classLoader.getResource("example-config.yaml")!!.readText())
    val searchPatternProvider = SearchPatternProvider(repoConfig.searchPaths, repoConfig.updateRules, dependencyKinds)

    val searchPattern = searchPatternProvider.resolveForKind(kind)
    val files = fileFinder.find(searchPattern)

    val updateSuggestionsMapper = UpdateSuggestionsMapper(
      searchPatternProvider,
      fileFinder,
      files
    )
    return updateSuggestionsMapper.map(updateSuggestions)
  }
}
