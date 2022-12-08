package org.virtuslab.bazelsteward.maven

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.core.library.SimpleVersion

class MavenCoordinatesTest {
  @Test
  fun `should have equals`() {
    val fromFactory = MavenCoordinates.of("org.virtuslab.ideprobe", "driver_2.12", "0.47.0")
    val fromConstructor =
      MavenCoordinates(MavenLibraryId("org.virtuslab.ideprobe", "driver_2.12"), SimpleVersion("0.47.0"))
    fromFactory shouldBe fromConstructor
  }
}
