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
    SemanticVersion.fromString("1.2") shouldBe None
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
