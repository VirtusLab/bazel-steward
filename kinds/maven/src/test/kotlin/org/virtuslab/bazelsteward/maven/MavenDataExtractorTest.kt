package org.virtuslab.bazelsteward.maven

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MavenDataExtractorTest {
  @Test
  fun `should parse coordinates`() {
    MavenDataExtractor.parseCoordinates(
      listOf(
        "com.carrotsearch:hppc:0.11.1",
        "com.google.guava:guava:32.0.1-jre",
      ),
    ) shouldBe listOf(
      MavenCoordinates.of("com.carrotsearch", "hppc", "0.11.1"),
      MavenCoordinates.of("com.google.guava", "guava", "32.0.1-jre"),
    )
  }

  @Test
  fun `should skip artifacts whose version comes from a bom`() {
    MavenDataExtractor.parseCoordinates(
      listOf(
        "com.fasterxml.jackson.core:jackson-core:",
        "com.google.guava:guava-testlib:",
        "com.carrotsearch:hppc:0.11.1",
      ),
    ) shouldBe listOf(
      MavenCoordinates.of("com.carrotsearch", "hppc", "0.11.1"),
    )
  }

  @Test
  fun `should ignore blank lines`() {
    MavenDataExtractor.parseCoordinates(listOf("")) shouldBe emptyList()
  }
}
