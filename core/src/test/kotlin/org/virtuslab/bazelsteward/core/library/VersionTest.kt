package org.virtuslab.bazelsteward.core.library

import arrow.core.None
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionTest {
  private val notSemVerVersions = listOf(
    "1.0a1-SNAPSHOT+whatever",
    "1.0+whatever",
    "1.final",
    "1.final-2a",
    "1.0-0",
    "1.final+whatever",
    "1.0-alpha1",
    "1.0beta1-SNAPSHOT",
    "1.0-b2",
    "1.0-beta3.SNAPSHOT",
    "1.0-beta3",
    "1.0-milestone1-SNAPSHOT",
    "1.0-m2",
    "1.0-rc1-SNAPSHOT",
    "1.0-cr1",
    "1.0-SNAPSHOT",
    "1.0",
    "1.0-sp",
    "1.0-a",
    "1.0-RELEASE",
    "1.0-whatever",
    "1.0.z",
    "1.0.1.0.0.0.0.0.0.0.0.0.0.0.1"
  )

  private fun singleArguments(): Stream<String> {
    return notSemVerVersions.stream()
  }

//  @Test
  @ParameterizedTest
  @MethodSource("singleArguments")
  fun `not SemVer Version`(version: String) {
      val ver = SimpleVersion(version)
      ver.toSemVer() shouldBe None
  }

}