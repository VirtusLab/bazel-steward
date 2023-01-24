package org.virtuslab.bazelsteward.core.library

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SemanticVersionTest {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class StrictSemVerVersioningTypeTest {

    private val strictVersioningType = VersioningSchema(VersioningType.SEMVER.name)
    @ParameterizedTest
    @MethodSource("strictVersioningArguments")
    fun `test fromString`(value: String, major: Int, minor: Int, patch: Int, prerelease: String, buildMetadata: String) {
      testSemanticVersion(value, major, minor, patch, prerelease, buildMetadata, strictVersioningType)
    }

    private fun strictVersioningArguments(): List<Arguments> = listOf(
      Arguments.of("1.2.3", 1, 2, 3, "", ""),
      Arguments.of("2.0.0-alpha+beta", 2, 0, 0, "alpha", "beta"),
      Arguments.of("1.0.1", 1, 0, 1, "", ""),
      Arguments.of("1.0.0-alpha", 1, 0, 0, "alpha", ""),
      Arguments.of("1.0.0-0.3.7", 1, 0, 0, "0.3.7", ""),
      Arguments.of("1.0.0-x.7.z.92", 1, 0, 0, "x.7.z.92", ""),
      Arguments.of("1.0.0-alpha+001", 1, 0, 0, "alpha", "001"),
      Arguments.of("1.0.0+20130313144700", 1, 0, 0, "", "20130313144700"),
      Arguments.of("1.0.0-beta+exp.sha.5114f85", 1, 0, 0, "beta", "exp.sha.5114f85"),
      Arguments.of("1.0.0-alpha", 1, 0, 0, "alpha", ""),
      Arguments.of("1.0.0-alpha.1", 1, 0, 0, "alpha.1", ""),
      Arguments.of("1.0.0-alpha.beta", 1, 0, 0, "alpha.beta", ""),
      Arguments.of("1.0.0-beta", 1, 0, 0, "beta", ""),
      Arguments.of("1.0.0-beta.2", 1, 0, 0, "beta.2", ""),
      Arguments.of("1.0.0-beta.11", 1, 0, 0, "beta.11", ""),
      Arguments.of("1.0.0-rc.1", 1, 0, 0, "rc.1", ""),
      Arguments.of("1.0.0-rc.1", 1, 0, 0, "rc.1", ""),
      Arguments.of("1.0.0", 1, 0, 0, "", ""),
    )

    @Test
    fun `test fromString wrong versions`() {
      SemanticVersion.fromString("1!1.0", strictVersioningType) shouldBe null
      SemanticVersion.fromString("NotAVersionSting", strictVersioningType) shouldBe null
    }
  }

  @Nested
  inner class LooseVersioningTypeTest {
    // TODO
  }

  fun testSemanticVersion(
    value: String,
    major: Int,
    minor: Int,
    patch: Int,
    prerelease: String,
    buildMetadata: String,
    versioningSchema: VersioningSchema
  ) {
    val sevVer = SemanticVersion.fromString(value, versioningSchema)
    sevVer shouldBe SemanticVersion(major, minor, patch, prerelease, buildMetadata)
    sevVer!!.value shouldBe value
  }
}
