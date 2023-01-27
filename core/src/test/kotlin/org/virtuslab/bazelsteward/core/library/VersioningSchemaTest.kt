package org.virtuslab.bazelsteward.core.library

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class VersioningSchemaTest {

  @Test
  fun `should throw an exception when versioningSchema with double group names is created`() {
    Assertions.assertThatThrownBy { VersioningSchema.Regex("^(?<major>\\d*)(?:[.-](?<major>(\\d*)))?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()) }
      .hasMessageContaining("Named capturing group <major> is already defined near index 29")
  }

  @Test
  fun `should throw an exception when versioningSchema is created with regex that doesn't contain all the groups`() {
    Assertions.assertThatThrownBy { VersioningSchema.Regex("^(?<major>\\d*)?(?:[.-]?(?<patch>(\\d*)))?(?:[-.]?(?<preRelease>(\\d*)))(?<buildMetaData>)?".toRegex()) }
      .hasMessageContaining("does not contain all required groups: <major>, <minor>, <patch>, <preRelease>, <buildMetaData>")
  }
}
