package org.virtuslab.bazelsteward.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VersionTest {
    @Test
    fun `should have value`() {
        Version("3.0.0").value shouldBe "3.0.0"
    }

}
