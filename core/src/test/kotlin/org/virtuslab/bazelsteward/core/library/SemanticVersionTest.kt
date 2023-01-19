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
    testSemanticVersion("1.0.0-x.7.z.92", 1, 0, 0, "x.7.z.92", "")
    testSemanticVersion("1.0.0-alpha+001", 1, 0, 0, "alpha", "001")
    testSemanticVersion("1.0.0+20130313144700", 1, 0, 0, "", "20130313144700")
    testSemanticVersion("1.0.0-beta+exp.sha.5114f85", 1, 0, 0, "beta", "exp.sha.5114f85")
    testSemanticVersion("1.0.0-alpha", 1, 0, 0, "alpha", "")
    testSemanticVersion("1.0.0-alpha.1", 1, 0, 0, "alpha.1", "")
    testSemanticVersion("1.0.0-alpha.beta", 1, 0, 0, "alpha.beta", "")
    testSemanticVersion("1.0.0-beta", 1, 0, 0, "beta", "")
    testSemanticVersion("1.0.0-beta.2", 1, 0, 0, "beta.2", "")
    testSemanticVersion("1.0.0-beta.11", 1, 0, 0, "beta.11", "")
    testSemanticVersion("1.0.0-rc.1", 1, 0, 0, "rc.1", "")
    testSemanticVersion("1.0.0", 1, 0, 0, "", "")
  }

  @Test
  fun `test fromString wrong versions`() {
    SemanticVersion.fromString("1!1.0") shouldBe None
    SemanticVersion.fromString("NotAVersionSting") shouldBe None
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
