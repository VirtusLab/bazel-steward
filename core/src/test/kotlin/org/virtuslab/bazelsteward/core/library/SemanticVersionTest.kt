package org.virtuslab.bazelsteward.core.library

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SemanticVersionTest {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class StrictSemVerVersioningTypeTest {

    private val strictVersioningType = VersioningSchema.SemVer
    @ParameterizedTest
    @MethodSource("argumentsForStrictVersioning")
    fun `test fromString`(value: String, major: Int, minor: Int, patch: Int, prerelease: String, buildMetadata: String) {
      testSemanticVersion(
        value = value,
        major = major,
        minor = minor,
        patch = patch,
        prerelease = prerelease,
        buildMetadata = buildMetadata,
        versioningSchema = strictVersioningType
      )
    }

    private fun argumentsForStrictVersioning(): List<Arguments> = listOf(
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

    @ParameterizedTest
    @MethodSource("argumentsForWrongStrictVersioning")
    fun `test fromString with wrong versions`(value: String) {
      SemanticVersion.fromString(value, strictVersioningType) shouldBe null
    }

    private fun argumentsForWrongStrictVersioning(): List<String> =
      argumentsThatOnlyFitLooseVersioningType.map { it.get()[0] as String } + "1!1.0" + "NotAVersionSting"
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class LooseVersioningTypeTest {

    private val looseVersioningType = VersioningSchema.Loose
    @ParameterizedTest
    @MethodSource("argumentsForLooseVersioning")
    fun `test fromString`(value: String, expectedValue: String, major: Int, minor: Int, patch: Int, prerelease: String, buildMetadata: String) {
      testSemanticVersion(value, expectedValue, major, minor, patch, prerelease, buildMetadata, looseVersioningType)
    }

    private fun argumentsForLooseVersioning(): List<Arguments> = argumentsThatOnlyFitLooseVersioningType

    @ParameterizedTest
    @MethodSource("argumentsForCompareToVersions")
    fun `test compareTo`(first: String, second: String) {
      val firstVer = SemanticVersion.fromString(first, looseVersioningType)!!
      val secondVer = SemanticVersion.fromString(second, looseVersioningType)!!
      firstVer.compareTo(secondVer) shouldBeGreaterThan 0
    }

    private fun argumentsForCompareToVersions(): List<Arguments> {
      val arguments = argumentsThatOnlyFitLooseVersioningType
      val argumentsForCompareTo: MutableList<Arguments> = mutableListOf()
      for (i in arguments.indices step 2) {
        argumentsForCompareTo.add(Arguments.of(arguments[i].get()[0], arguments[i + 1].get()[0]))
      }
      return argumentsForCompareTo
    }
  }

  fun testSemanticVersion(
    value: String,
    expectedValue: String = value,
    major: Int,
    minor: Int,
    patch: Int,
    prerelease: String,
    buildMetadata: String,
    versioningSchema: VersioningSchema
  ) {
    val sevVer = SemanticVersion.fromString(value, versioningSchema)
    sevVer shouldBe SemanticVersion(major, minor, patch, prerelease, buildMetadata)
    sevVer!!.value shouldBe expectedValue
  }

  private val argumentsThatOnlyFitLooseVersioningType: List<Arguments> = listOf(
    Arguments.of("1.0+whatever", "1.0.0+whatever", 1, 0, 0, "", "whatever"),
    Arguments.of("1.0a1-SNAPSHOT+whatever", "1.0.0-a1-SNAPSHOT+whatever", 1, 0, 0, "a1-SNAPSHOT", "whatever"),
    Arguments.of("1.final-2a", "1.0.0-final-2a", 1, 0, 0, "final-2a", ""),
    Arguments.of("1.final", "1.0.0-final", 1, 0, 0, "final", ""),
    Arguments.of("1.final1+whatever", "1.0.0-final1+whatever", 1, 0, 0, "final1", "whatever"),
    Arguments.of("1.0-0", "1.0.0", 1, 0, 0, "", ""),
    Arguments.of("1.0beta1-SNAPSHOT", "1.0.0-beta1-SNAPSHOT", 1, 0, 0, "beta1-SNAPSHOT", ""),
    Arguments.of("1.0-alpha1", "1.0.0-alpha1", 1, 0, 0, "alpha1", ""),
    Arguments.of("1.0-beta3.SNAPSHOT", "1.0.0-beta3.SNAPSHOT", 1, 0, 0, "beta3.SNAPSHOT", ""),
    Arguments.of("1.0-b2", "1.0.0-b2", 1, 0, 0, "b2", ""),
    Arguments.of("1.0-milestone1-SNAPSHOT", "1.0.0-milestone1-SNAPSHOT", 1, 0, 0, "milestone1-SNAPSHOT", ""),
    Arguments.of("1.0-beta3", "1.0.0-beta3", 1, 0, 0, "beta3", ""),
    Arguments.of("1.0-rc1-SNAPSHOT", "1.0.0-rc1-SNAPSHOT", 1, 0, 0, "rc1-SNAPSHOT", ""),
    Arguments.of("1.0-m2", "1.0.0-m2", 1, 0, 0, "m2", ""),
    Arguments.of("1.0-SNAPSHOT", "1.0.0-SNAPSHOT", 1, 0, 0, "SNAPSHOT", ""),
    Arguments.of("1.0-cr1", "1.0.0-cr1", 1, 0, 0, "cr1", ""),
    Arguments.of("1.0-sp", "1.0.0-sp", 1, 0, 0, "sp", ""),
    Arguments.of("1.0", "1.0.0", 1, 0, 0, "", ""),
    Arguments.of("1.0-RELEASE", "1.0.0-RELEASE", 1, 0, 0, "RELEASE", ""),
    Arguments.of("1.0-a", "1.0.0-a", 1, 0, 0, "a", ""),
    Arguments.of("1.0.z", "1.0.0-z", 1, 0, 0, "z", ""),
    Arguments.of("1.0-whatever", "1.0.0-whatever", 1, 0, 0, "whatever", ""),
    Arguments.of("1-1.foo-bar-1-baz-0.1", "1.1.0-foo-bar-1-baz-0.1", 1, 1, 0, "foo-bar-1-baz-0.1", ""),
    Arguments.of("1.0.1.0.0.0.0.0.0.0.0.0.0.0.1", "1.0.1-0.0.0.0.0.0.0.0.0.0.0.1", 1, 0, 1, "0.0.0.0.0.0.0.0.0.0.0.1", ""),
    Arguments.of("2022.11.29.0.1-api-version-222", "2022.11.29-0.1-api-version-222", 2022, 11, 29, "0.1-api-version-222", ""),
    Arguments.of("1.0.0b1", "1.0.0-b1", 1, 0, 0, "b1", ""),
    Arguments.of("7.10-final", "7.10.0-final", 7, 10, 0, "final", ""),
    Arguments.of("7.9-628", "7.9.628", 7, 9, 628, "", ""),
    Arguments.of("7.9-sp", "7.9.0-sp", 7, 9, 0, "sp", ""),
    Arguments.of("7.9", "7.9.0", 7, 9, 0, "", ""),
  )
}
