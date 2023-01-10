package org.virtuslab.bazelsteward.core.library

import arrow.core.None
import arrow.core.Some
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SemanticVersionTest {


  @Test
  fun `test fromString`() {
    testSemanticVersion("1.2.3", 1, 2, 3, "", "")
    testSemanticVersion("2.0.0-alpha+beta", 2, 0, 0, "alpha", "beta")
    testSemanticVersion("1.0.1", 1, 0, 1, "", "")
    testSemanticVersion("1.0.0-alpha", 1, 0, 0, "alpha", "")
    testSemanticVersion("1.0.0-alpha.1", 1, 0, 0, "alpha.1", "")
    testSemanticVersion("1.0.0-0.3.7", 1, 0, 0, "0.3.7", "")
    testSemanticVersion("1.0.0-x.7.z.92", 1, 0, 0, "x.7.z.92","")
    testSemanticVersion("1.0.0-alpha+001", 1, 0, 0, "alpha", "001")
    testSemanticVersion("1.0.0+20130313144700", 1, 0, 0, "", "20130313144700")
    testSemanticVersion("1.0.0-beta+exp.sha.5114f85", 1, 0, 0, "beta", "exp.sha.5114f85")
    testSemanticVersion("1.0.0-alpha", 1, 0, 0, "alpha", "")
    testSemanticVersion("1.0.0-alpha.1", 1, 0, 0, "alpha.1", "")
    testSemanticVersion( "1.0.0-alpha.beta", 1, 0, 0, "alpha.beta", "")
    testSemanticVersion("1.0.0-beta", 1, 0, 0, "beta", "")
    testSemanticVersion("1.0.0-beta.2", 1, 0, 0, "beta.2", "")
    testSemanticVersion("1.0.0-beta.11",1, 0, 0, "beta.11", "")
    testSemanticVersion("1.0.0-rc.1", 1, 0, 0, "rc.1", "")
    testSemanticVersion("1.0.0", 1, 0, 0, "", "")
  }

  @Test
  fun `test fromString wrong versions`() {
    SemanticVersion.fromString("1.2") shouldBe None
    SemanticVersion.fromString("1") shouldBe None
    SemanticVersion.fromString("7.9-628") shouldBe None
    SemanticVersion.fromString("2022.11.29.0.1-api-version-222") shouldBe None
    SemanticVersion.fromString("1!1.0") shouldBe None
    SemanticVersion.fromString("1.0.0b1") shouldBe None

    SemanticVersion.fromString("1.0a1-SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0-alpha1") shouldBe None
    SemanticVersion.fromString("1.0beta1-SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0-b2") shouldBe None
    SemanticVersion.fromString("1.0-beta3.SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0-beta3") shouldBe None
    SemanticVersion.fromString("1.0-milestone1-SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0-m2") shouldBe None
    SemanticVersion.fromString("1.0-rc1-SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0-cr1") shouldBe None
    SemanticVersion.fromString("1.0-SNAPSHOT") shouldBe None
    SemanticVersion.fromString("1.0") shouldBe None
    SemanticVersion.fromString("1.0-sp") shouldBe None
    SemanticVersion.fromString("1.0-a") shouldBe None
    SemanticVersion.fromString("1.0-RELEASE") shouldBe None
    SemanticVersion.fromString("1.0-whatever") shouldBe None
    SemanticVersion.fromString("1.0.z") shouldBe None
    SemanticVersion.fromString("1.0.1.0.0.0.0.0.0.0.0.0.0.0.1") shouldBe None
    SemanticVersion.fromString("1-1.foo-bar-1-baz-0.1") shouldBe None
  }

  private fun testSemanticVersion(
    value: String,
    major: Int,
    minor: Int,
    patch: Int,
    prerelease: String,
    buildMetadata: String
  ) {
    val sevVer = SemanticVersion.fromString(value)
    sevVer shouldBe Some(
      SemanticVersion(
        major,
        minor,
        patch,
        prerelease,
        buildMetadata
      )
    )
    sevVer.tap { it.value shouldBe value }
  }
}
