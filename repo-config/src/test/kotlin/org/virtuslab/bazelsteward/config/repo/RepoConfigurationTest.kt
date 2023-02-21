package org.virtuslab.bazelsteward.config.repo

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import java.nio.file.Path

class RepoConfigurationTest {

  @Test
  fun `should throw an exception when maven object in config file is not correct`(@TempDir tempDir: Path) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail.yaml")
    Assertions.assertThatThrownBy { loadConfig(tempDir) }
      .hasMessage(
        listOf(
          "update-rules[0].dependenciess: is not defined in the schema and the schema does not allow additional properties",
          "update-rules[1].versioning: does not have a value in the enumeration [loose, semver]",
          "update-rules[1].versioning: does not match the regex pattern ^regex:",
          "update-rules[1].bumping: does not have a value in the enumeration [default, latest]",
          "update-rules[2].kinds[0]: integer found, string expected",
        ).joinToString(System.lineSeparator())
      )
  }

  @Test
  fun `should throw an exception when versioning regex does not contain all required named groups`(@TempDir tempDir: Path) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail2.yaml")
    Assertions.assertThatThrownBy { loadConfig(tempDir) }
      .hasMessageContaining("does not contain all required groups: <major>, <minor>, <patch>, <preRelease>, <buildMetaData>")
  }

  @Test
  fun `should create default configuration when config file is not declared`(@TempDir tempDir: Path) {
    val configuration = loadConfig(tempDir)
    Assertions.assertThat(configuration).usingRecursiveComparison().isEqualTo(RepoConfig())
  }

  @Test
  fun `should create configuration when config file is correct`(@TempDir tempDir: Path) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-correct.yaml")
    val configuration = loadConfig(tempDir)
    val expectedConfiguration = RepoConfig(
      listOf(
        UpdateRulesConfig(
          kinds = listOf("maven"),
          dependencies = listOf(DependencyNameFilter.Default("commons-io:commons-io")),
          versioning = VersioningSchema.Loose,
          bumping = BumpingStrategy.Default,
          pin = PinningStrategy.Prefix("2.0.")
        ),
        UpdateRulesConfig(
          dependencies = listOf(DependencyNameFilter.Default("io.get-coursier:interface")),
          versioning = VersioningSchema.SemVer,
          bumping = BumpingStrategy.Latest,
        ),
        UpdateRulesConfig(
          dependencies = listOf(DependencyNameFilter.Default("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")),
          versioning = VersioningSchema.Regex("^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()),
        ),
        UpdateRulesConfig(
          dependencies = listOf(DependencyNameFilter.Default("org.jetbrains.kotlinx:*")),
          versioning = VersioningSchema.Loose,
        ),
        UpdateRulesConfig(
          versioning = VersioningSchema.Loose,
        ),
      ),
    )
    Assertions.assertThat(configuration).isEqualTo(expectedConfiguration)
  }

  private fun loadConfig(tempDir: Path): RepoConfig {
    return runBlocking { RepoConfigParser().load(tempDir.resolve(".bazel-steward.yaml")) }
  }

  private fun copyConfigFileToTempLocation(tempDir: Path, configFileName: String) {
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource(configFileName),
      tempDir.resolve(".bazel-steward.yaml").toFile()
    )
  }
}
