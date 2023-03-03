package org.virtuslab.bazelsteward.core

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PathPatternTest {

  @Test
  fun `should return glob empty argument null`() {
    Assertions.assertThatThrownBy { PathPattern.parse(null) }
      .hasMessageContaining("Wrong search-pattern")
  }

  @Test
  fun `should return glob empty argument empty`() {
    Assertions.assertThatThrownBy { PathPattern.parse("") }
      .hasMessageContaining("Wrong search-pattern")
  }

  private fun argumentsForCorrectPathPattern(): List<Arguments> = listOf(
    Arguments.of("glob: **/*.bzl", PathPattern.Glob("**/*.bzl")),
    Arguments.of("glob:**/*.bzl", PathPattern.Glob("**/*.bzl")),
    Arguments.of("exact: WORKSPACE", PathPattern.Exact("WORKSPACE")),
    Arguments.of("exact:WORKSPACE", PathPattern.Exact("WORKSPACE")),
    Arguments.of("regex: BUILD[\\.bzl]*", PathPattern.Regex("""BUILD[\.bzl]*""")),
    Arguments.of("regex:BUILD[\\.bzl]*", PathPattern.Regex("""BUILD[\.bzl]*""")),
    Arguments.of("**/*.bzl", PathPattern.Glob("**/*.bzl")),
    Arguments.of("BUILD[\\.bzl]?", PathPattern.Regex("BUILD[\\.bzl]?")),
    Arguments.of("(BUILD[\\.bzl]*", PathPattern.Exact("(BUILD[\\.bzl]*")),
    Arguments.of("WORKSPACE", PathPattern.Exact("WORKSPACE")),
  )

  @ParameterizedTest
  @MethodSource("argumentsForCorrectPathPattern")
  fun `should return correct PathPattern`(pin: String, expectedStrategy: PathPattern) {
    val result = PathPattern.parse(pin)
    result shouldBe expectedStrategy
  }
}
