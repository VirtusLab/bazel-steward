package org.virtuslab.bazelsteward.core.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PinningStrategyTest {

  @Test
  fun `should return null argument null`() {
    val result = PinningStrategy.parse(null)
    result shouldBe null
  }

  @Test
  fun `should return null argument empty`() {
    val result = PinningStrategy.parse("")
    result shouldBe null
  }

  private fun argumentsForCorrectPinningStrategy(): List<Arguments> = listOf(
    Arguments.of("prefix: 2.0.", PinningStrategy.Prefix("2.0.")),
    Arguments.of("prefix:2.0.", PinningStrategy.Prefix("2.0.")),
    Arguments.of("exact: 2.0.0", PinningStrategy.Exact("2.0.0")),
    Arguments.of("exact:2.0.0", PinningStrategy.Exact("2.0.0")),
    Arguments.of("regex: \$|()?^*{}", PinningStrategy.Regex("\$|()?^*{}")),
    Arguments.of("regex:\$|()?^*{}", PinningStrategy.Regex("\$|()?^*{}")),
    Arguments.of("2.0.", PinningStrategy.Prefix("2.0.")),
    Arguments.of("\$|()?^*{}", PinningStrategy.Regex("\$|()?^*{}")),
    Arguments.of("2.0.", PinningStrategy.Prefix("2.0.")),
    Arguments.of("2.0.0", PinningStrategy.Exact("2.0.0")),
  )

  @ParameterizedTest
  @MethodSource("argumentsForCorrectPinningStrategy")
  fun `should return correct PinningStrategy`(pin: String, expectedStrategy: PinningStrategy) {
    val result = PinningStrategy.parse(pin)
    result shouldBe expectedStrategy
  }
}
