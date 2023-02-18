package org.virtuslab.bazelsteward.config.repo

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import java.io.File

class RepoConfigurationTest {

  @Test
  fun `should throw an exception when maven object in config file is not correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail.yaml")
    Assertions.assertThatThrownBy { runBlocking { RepoConfigParser(tempDir.toPath().resolve(".bazel-steward.yaml")).get() } }
      .hasMessage(
        listOf(
          "maven.configs[0].grouop: is not defined in the schema and the schema does not allow additional properties",
          "maven.configs[0].artifactt: is not defined in the schema and the schema does not allow additional properties",
          "maven.configs[1].versioning: does not have a value in the enumeration [loose, semver]",
          "maven.configs[1].versioning: does not match the regex pattern ^regex:",
          "maven.configs[1].bumping: does not have a value in the enumeration [default, latest]",
          "maven.configs[2].group: integer found, string expected",
        ).joinToString(System.lineSeparator())
      )
  }

  @Test
  fun `should throw an exception when versioning regex does not contain all required named groups`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail2.yaml")
    Assertions.assertThatThrownBy { runBlocking { RepoConfigParser(tempDir.toPath().resolve(".bazel-steward.yaml")).get() } }
      .hasMessageContaining("does not contain all required groups: <major>, <minor>, <patch>, <preRelease>, <buildMetaData>")
  }

  @Test
  fun `should create default configuration when config file is not declared`(@TempDir tempDir: File) {
    val configuration = runBlocking { RepoConfigParser(tempDir.toPath().resolve(".bazel-steward.yaml")).get() }
    Assertions.assertThat(configuration).usingRecursiveComparison().isEqualTo(RepoConfig())
  }

  @Test
  fun `should create configuration when config file is correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-correct.yaml")
    val configuration = runBlocking { RepoConfigParser(tempDir.toPath().resolve(".bazel-steward.yaml")).get() }
    val expectedConfiguration = RepoConfig(
      MavenConfig(
        listOf(
          ConfigEntry("commons-io", "commons-io", null, VersioningSchema.Loose, BumpingStrategy.Default),
          ConfigEntry("io.get-coursier", "interface", null, VersioningSchema.SemVer, BumpingStrategy.Latest),
          ConfigEntry("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", null, VersioningSchema.Regex("^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()), null),
          ConfigEntry("org.jetbrains.kotlinx", null, null, VersioningSchema.Loose, null),
          ConfigEntry(null, null, null, VersioningSchema.Loose, null),
        ),
      )
    )
    Assertions.assertThat(configuration).isEqualTo(expectedConfiguration)
  }

  @Test
  fun `should create configuration when config file is correct with pin version`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-pin.yaml")
    val configuration = runBlocking { RepoConfigParser(tempDir.toPath().resolve(".bazel-steward.yaml")).get() }
    val expectedConfiguration = RepoConfig(
      MavenConfig(
        listOf(
          ConfigEntry("commons-io", "commons-io", PinningStrategy.Prefix("2."), VersioningSchema.Loose, BumpingStrategy.Default),
          ConfigEntry("io.get-coursier", "interface", PinningStrategy.Prefix("1.0."), VersioningSchema.SemVer, BumpingStrategy.Latest),
          ConfigEntry("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", PinningStrategy.Prefix("1."), VersioningSchema.Regex("^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()), null),
          ConfigEntry("org.jetbrains.kotlinx", null, null, VersioningSchema.Loose, null),
          ConfigEntry(null, null, null, VersioningSchema.Loose, null),
        ),
      )
    )
    Assertions.assertThat(configuration).isEqualTo(expectedConfiguration)
  }

  private fun copyConfigFileToTempLocation(tempDir: File, configFileName: String) {
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource(configFileName),
      File(tempDir, ".bazel-steward.yaml")
    )
  }
}
