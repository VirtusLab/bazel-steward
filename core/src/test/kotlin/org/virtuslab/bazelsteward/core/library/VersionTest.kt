package org.virtuslab.bazelsteward.core.library

import arrow.core.None
import arrow.core.compareTo
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionTest {
  private val mavenVersions = listOf(
    "1.0+whatever",
    "1.0a1-SNAPSHOT+whatever",
    "1.final-2a",
    "1.final",
    "1.final1+whatever",
    "1.0-0",
    "1.0beta1-SNAPSHOT",
    "1.0-alpha1",
    "1.0-beta3.SNAPSHOT",
    "1.0-b2",
    "1.0-milestone1-SNAPSHOT",
    "1.0-beta3",
    "1.0-rc1-SNAPSHOT",
    "1.0-m2",
    "1.0-SNAPSHOT",
    "1.0-cr1",
    "1.0-sp",
    "1.0",
    "1.0-RELEASE",
    "1.0-a",
    "1.0.z",
    "1.0-whatever",
    "1-1.foo-bar-1-baz-0.1",
    "1.0.1.0.0.0.0.0.0.0.0.0.0.0.1",
    "2022.11.29.0.1-api-version-222",
    "1.0.0b1",
    "7.10-final",
    "7.9-628",
    "7.9-sp",
    "7.9",
  )

  private fun argumentsForCheckVersion(): Stream<String> {
    return mavenVersions.stream()
  }

  @ParameterizedTest
  @MethodSource("argumentsForCheckVersion")
  fun `check version`(version: String) {
    val ver = SimpleVersion(version)
    ver.toSemVer() shouldNotBe None
  }

  private fun argumentsForCompareVersions(): Stream<Arguments> {
    val listWithArguments: MutableList<Arguments> = mutableListOf()
    for (i in mavenVersions.indices step 2) {
      listWithArguments.add(Arguments.of(mavenVersions[i], mavenVersions[i + 1]))
    }
    return listWithArguments.stream()
  }

  @ParameterizedTest
  @MethodSource("argumentsForCompareVersions")
  fun `compare Versions`(first: String, second: String) {
    val firstVer = SimpleVersion(first).toSemVer()
    val secondVer = SimpleVersion(second).toSemVer()
    firstVer.compareTo(secondVer) shouldBeGreaterThan 0
  }
}
