package org.virtuslab.bazelsteward.app

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.common.UpdateRules
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
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
    val updateRules = UpdateRules(bumpingStrategy = BumpingStrategy.Default)
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
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
    val updateRules = UpdateRules(bumpingStrategy = BumpingStrategy.Latest)
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
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
  fun `should selectUpdate test with mixed bumping strategies`(
    group: String,
    artifact: String,
    version: String,
    suggestion: String,
    versioningSchema: VersioningSchema,
    bumpingStrategy: BumpingStrategy
  ) {
    val coordinates = MavenCoordinates.of(group, artifact, version)
    val updateRules = UpdateRules(versioningSchema, bumpingStrategy)
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateMix(): List<Arguments> = listOf(
    Arguments.of("g1", "a1", "2.0.0", "2.0.2", VersioningSchema.Loose, BumpingStrategy.Default),
    Arguments.of("g1", "a4", "2.0.2+beta", "2.3.3+beta", VersioningSchema.Loose, BumpingStrategy.Default),
    Arguments.of("g2", "a1", "2.0.2", "2.3.3+beta", VersioningSchema.SemVer, BumpingStrategy.Default),
    Arguments.of("g2", "a2", "3.0.1", "4.0.2", VersioningSchema.Loose, BumpingStrategy.Latest),
    Arguments.of("g3", "a1", "2.1.0+beta", "4.0.2", VersioningSchema.Loose, BumpingStrategy.Latest),
    Arguments.of("g3", "a3", "2.3.2+beta", "2.3.3+beta", VersioningSchema.Loose, BumpingStrategy.Default),
    Arguments.of("g3", "a5", "3.0.0+beta", "4.0.2", VersioningSchema.Loose, BumpingStrategy.Latest),
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdatePrefixPinned")
  fun `should selectUpdate test with pinned prefix version`(version: String, pin: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of("group", "artifact", version)
    val updateRules = UpdateRules(bumpingStrategy = BumpingStrategy.Latest, pinningStrategy = PinningStrategy.Prefix(pin))
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdatePrefixPinned(): List<Arguments> = listOf(
    Arguments.of("2.0.0", "2.", "2.3.3+beta"),
    Arguments.of("2.0.0", "2.2.1", "2.2.1"),
    Arguments.of("2.0.1+beta", "2.0", "2.0.2"),
    Arguments.of("2.0.0-alpha", "2.", null),
    Arguments.of("2.0.2+beta", "2.2", "2.2.8+beta"),
    Arguments.of("2.0.2", "2.3.0", "2.3.0+beta"),
    Arguments.of("2.1.0+beta", "2.", "2.3.3+beta"),
    Arguments.of("2.3.3", "2.", null),
    Arguments.of("2.3.2+beta", "2.", "2.3.3+beta"),
    Arguments.of("3.0.1", "3.", "3.2.1+beta"),
    Arguments.of("3.0.0+beta", "3.0", "3.0.1"),
    Arguments.of("4.0.0-alpha", "4.", null),
    Arguments.of("4.0.0", "4.0", "4.0.2"),
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdateExactPinned")
  fun `should selectUpdate test with pinned exact version`(version: String, pin: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of("group", "artifact", version)
    val updateRules = UpdateRules(bumpingStrategy = BumpingStrategy.Latest, pinningStrategy = PinningStrategy.Exact(pin))
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateExactPinned(): List<Arguments> = listOf(
    Arguments.of("2.0.0", "2.3.2+beta", "2.3.2+beta"),
    Arguments.of("2.0.1+beta", "2.0.2", "2.0.2"),
    Arguments.of("2.0.0-alpha", "5.0.0", null),
    Arguments.of("2.0.2+beta", "2.2", null)
  )

  @ParameterizedTest
  @MethodSource("argumentsForSelectUpdateRegexPinned")
  fun `should selectUpdate test with pinned regex version`(version: String, pin: String, suggestion: String?) {
    val coordinates = MavenCoordinates.of("group", "artifact", version)
    val updateRules = UpdateRules(bumpingStrategy = BumpingStrategy.Latest, pinningStrategy = PinningStrategy.Regex(pin))
    val updateSuggestion = UpdateLogic().selectUpdate(coordinates, availableVersions, updateRules)
    Assertions.assertThat(updateSuggestion?.suggestedVersion?.value).isEqualTo(suggestion)
  }

  private fun argumentsForSelectUpdateRegexPinned(): List<Arguments> = listOf(
    Arguments.of("2.0.0", "^2\\.\\d?\\.0\$", "2.2.0"),
    Arguments.of("2.0.0", "^2\\.\\d?.\\d?\$", "2.2.1"),
    Arguments.of("2.0.1+beta", "2.0", null),
    Arguments.of("2.0.0-alpha", "2.", null),
  )
}
