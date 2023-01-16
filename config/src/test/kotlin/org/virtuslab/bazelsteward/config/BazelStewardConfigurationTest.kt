package org.virtuslab.bazelsteward.config

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.io.File

class BazelStewardConfigurationTest {

  @Test
  fun `should throw exception when maven object in config file is not correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-fail.yaml")
    Assertions.assertThatThrownBy { runBlocking { BazelStewardConfiguration(tempDir.toPath()).get() } }
      .hasMessage(
        listOf(
          "maven.ruledDependencies[0].id.group: is missing but it is required",
          "maven.ruledDependencies[0].id.artifact: is missing but it is required",
          "maven.ruledDependencies[1].versioning: does not have a value in the enumeration [loose, semver]",
          "maven.ruledDependencies[1].versioning: does not match the regex pattern ^regex:",
          "maven.ruledDependencies[2].id.group: integer found, string expected"
        ).joinToString(System.lineSeparator())
      )
  }

  @Test
  fun `should create default configuration when config file is not declared`(@TempDir tempDir: File) {
    val configuration = runBlocking { BazelStewardConfiguration(tempDir.toPath()).get() }
    Assertions.assertThat(configuration).isEqualTo(Configuration())
  }

  @Test
  fun `should create configuration when config file is correct`(@TempDir tempDir: File) {
    copyConfigFileToTempLocation(tempDir, ".bazel-steward-correct.yaml")
    val configuration = runBlocking { BazelStewardConfiguration(tempDir.toPath()).get() }
    val expectedConfiguration = Configuration(
      MavenConfig(
        listOf(
          MavenDependency(MavenLibraryId("commons-io", "commons-io"), "loose"),
          MavenDependency(MavenLibraryId("io.get-coursier", "interface"), "semver"),
          MavenDependency(MavenLibraryId("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8"), "regex:(?<major>\\d+)")
        )
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
