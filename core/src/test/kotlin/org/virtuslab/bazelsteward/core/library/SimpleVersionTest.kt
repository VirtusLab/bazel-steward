package org.virtuslab.bazelsteward.core.library

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SimpleVersionTest {
  @Test
  fun `should have value`() {
    SimpleVersion("3.0.0").value shouldBe "3.0.0"
  }
}
