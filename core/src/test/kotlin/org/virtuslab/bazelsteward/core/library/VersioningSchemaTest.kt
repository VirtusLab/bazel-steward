package org.virtuslab.bazelsteward.core.library

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class VersioningSchemaTest {

  @Test
  fun `should throw an exception when versioningSchema with double group names is created`() {
    Assertions.assertThatThrownBy { VersioningSchema("regex:^(?<major>\\d*)(?:[.-](?<major>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?") }
      .hasMessageContaining("Named capturing group <major> is already defined near index 35")
  }

  @Test
  fun `should throw an exception when versioningSchema is created with regex that doesn't contain all the groups`() {
    Assertions.assertThatThrownBy { VersioningSchema("regex:^(?<major>\\d*)?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?") }
      .hasMessageContaining("does not contain all required groups: <major>, <minor>, <patch>, <preRelease>, <buildMetaData>")
  }

  @Test
  fun `should throw an exception when versioningSchema is created with value that doesn't match enum`() {
    Assertions.assertThatThrownBy { VersioningSchema("undefined") }
      .hasMessage("Versioning schema must be either a custom regex or have one of two forms: semver, loose.\n")
  }

  @Test
  fun `should create versioningSchema when regex is correct`() {
    val regex = "regex:^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?"
    val versioningSchema = VersioningSchema("regex:^(?<major>\\d*)(?:[.-](?<minor>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?")
    Assertions.assertThat(versioningSchema)
      .hasFieldOrPropertyWithValue("type", VersioningType.REGEX)
      .hasFieldOrPropertyWithValue("regex", regex.removePrefix("regex:"))
  }

  @ParameterizedTest
  @EnumSource(value = VersioningType::class, names = ["SEMVER", "LOOSE"])
  fun `should create versioningSchema when passed value matches the enum`(type: VersioningType) {
    val versioningSchema = VersioningSchema(type.name)
    Assertions.assertThat(versioningSchema)
      .hasFieldOrPropertyWithValue("type", type)
      .hasFieldOrPropertyWithValue("regex", null)
  }
}
