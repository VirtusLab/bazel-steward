package org.virtuslab.bazelsteward.core.config

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import java.io.File

class BazelStewardConfigurationTest {

  @Test
  fun `should throw an exception when maven object in config file is not correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail.yaml")
    Assertions.assertThatThrownBy { runBlocking { BazelStewardConfigExtractor(tempDir.toPath()).get() } }
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
    Assertions.assertThatThrownBy { runBlocking { BazelStewardConfigExtractor(tempDir.toPath()).get() } }
      .hasMessageContaining("does not contain all required groups: <major>, <minor>, <patch>, <preRelease>, <buildMetaData>")
  }

  @Test
  fun `should create default configuration when config file is not declared`(@TempDir tempDir: File) {
    val configuration = runBlocking { BazelStewardConfigExtractor(tempDir.toPath()).get() }
    Assertions.assertThat(configuration).usingRecursiveComparison().isEqualTo(BazelStewardConfig())
  }

  @Test
  fun `should create configuration when config file is correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-correct.yaml")
    val configuration = runBlocking { BazelStewardConfigExtractor(tempDir.toPath()).get() }
    val expectedConfiguration = BazelStewardConfig(
      MavenConfig(
        listOf(
          ConfigEntry("commons-io", "commons-io", VersioningSchema.Loose, BumpingStrategy.Default),
          ConfigEntry("io.get-coursier", "interface", VersioningSchema.SemVer, BumpingStrategy.Latest),
          ConfigEntry("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", VersioningSchema.Regex("^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()), null),
          ConfigEntry("org.jetbrains.kotlinx", null, VersioningSchema.Loose, null),
          ConfigEntry(null, null, VersioningSchema.Loose, null),
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
