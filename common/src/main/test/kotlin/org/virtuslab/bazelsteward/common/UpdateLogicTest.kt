package common.src.main.test.kotlin.org.virtuslab.bazelsteward.common

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.config.BazelStewardConfig
import org.virtuslab.bazelsteward.config.BumpingStrategy
import org.virtuslab.bazelsteward.config.ConfigEntry
import org.virtuslab.bazelsteward.config.MavenConfig
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.maven.MavenCoordinates

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateLogicTest {

  private val availableVersions = listOf(
    SimpleVersion("2.0.1"),
    SimpleVersion("2.0.2"),
    SimpleVersion("2.0.3-alpha"),
    SimpleVersion("2.1.0"),
    SimpleVersion("3.2.1+beta"),
    SimpleVersion("2.2.1"),
    SimpleVersion("2.3.2+beta"),
    SimpleVersion("2.2.0"),
    SimpleVersion("2.3.0+beta"),
    SimpleVersion("3.0.1"),
    SimpleVersion("3.2.0+beta"),
    SimpleVersion("3.1.0"),
    SimpleVersion("2.1.6+beta"),
    SimpleVersion("2.3.3+beta"),
    SimpleVersion("4.0.1-alpha"),
    SimpleVersion("4.0.2"),
    SimpleVersion("2.2.8+beta"),
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdateDefault")
  fun `should selectUpdate test with default bumping strategy`(version: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of("group", "artifact", version)
    val updateSuggestion = UpdateLogic(BazelStewardConfig()).selectUpdate(coordinates, availableVersions)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateDefault(): List<Arguments> = listOf(
    Arguments.of("2.0.0", "2.0.2"),
    Arguments.of("2.0.1+beta", "2.0.2"),
    Arguments.of("2.0.0-alpha", null),
    Arguments.of("2.0.2+beta", "2.3.3+beta"),
    Arguments.of("2.0.2", "2.3.3+beta"),
    Arguments.of("2.1.0+beta", "2.1.6+beta"),
    Arguments.of("2.3.3", "4.0.2"),
    Arguments.of("2.3.2+beta", "2.3.3+beta"),
    Arguments.of("3.0.1", "3.2.1+beta"),
    Arguments.of("3.0.0+beta", "3.0.1"),
    Arguments.of("4.0.0-alpha", null),
    Arguments.of("4.0.0", "4.0.2"),
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdateLatest")
  fun `should selectUpdate test with latest bumping strategy`(version: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of("group", "artifact", version)
    val mavenConfig = MavenConfig(
      listOf(
        ConfigEntry(null, null, null, BumpingStrategy.LATEST)
      )
    )
    val updateSuggestion = UpdateLogic(BazelStewardConfig(mavenConfig)).selectUpdate(coordinates, availableVersions)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateLatest(): List<Arguments> = listOf(
    Arguments.of("2.0.0", "4.0.2"),
    Arguments.of("2.0.1+beta", "4.0.2"),
    Arguments.of("2.0.0-alpha", null),
    Arguments.of("2.0.2+beta", "4.0.2"),
    Arguments.of("2.0.2", "4.0.2"),
    Arguments.of("2.1.0+beta", "4.0.2"),
    Arguments.of("2.3.3", "4.0.2"),
    Arguments.of("2.3.2+beta", "4.0.2"),
    Arguments.of("3.0.1", "4.0.2"),
    Arguments.of("3.0.0+beta", "4.0.2"),
    Arguments.of("4.0.0-alpha", null),
    Arguments.of("4.0.0", "4.0.2"),
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdateMix")
  fun `should selectUpdate test with mixed bumping strategies`(group: String, artifact: String, version: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of(group, artifact, version)
    val bazelStewardConfig = BazelStewardConfig(
      MavenConfig(
        listOf(
          ConfigEntry("g2", "a1", VersioningSchema.Loose, BumpingStrategy.DEFAULT),
          ConfigEntry("g3", "a3", VersioningSchema.Loose, BumpingStrategy.DEFAULT),
          ConfigEntry("g1", null, null, BumpingStrategy.DEFAULT),
          ConfigEntry("g2", null, null, BumpingStrategy.LATEST),
          ConfigEntry(null, null, null, BumpingStrategy.LATEST),
        ),
      )
    )
    val updateSuggestion = UpdateLogic(bazelStewardConfig).selectUpdate(coordinates, availableVersions)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateMix(): List<Arguments> = listOf(
    Arguments.of("g1", "a1", "2.0.0", "2.0.2"),
    Arguments.of("g1", "a4", "2.0.2+beta", "2.3.3+beta"),
    Arguments.of("g2", "a1", "2.0.2", "2.3.3+beta"),
    Arguments.of("g2", "a2", "3.0.1", "4.0.2"),
    Arguments.of("g3", "a1", "2.1.0+beta", "4.0.2"),
    Arguments.of("g3", "a3", "2.3.2+beta", "2.3.3+beta"),
    Arguments.of("g3", "a5", "3.0.0+beta", "4.0.2"),
  )
}
