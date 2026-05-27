package org.virtuslab.bazelsteward.action

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReleaseTagResolverTest {
  private val fakeReleases = listOf(
    "v1.7.2-rc9\tRC",
    "v1.7.2\tRelease 1.7.2",
    "v1.7.2.1\tPatch",
    "v1.7.3\tRelease 1.7.3",
  )

  private val fakeGh = GhReleaseLister { fakeReleases }

  @Test
  fun `v prefix resolves to latest matching stable release`() {
    ReleaseTagResolver.resolveLatestMatchingRelease("v1.7", fakeReleases) shouldBe "v1.7.3"
  }

  @Test
  fun `exact v tag ref resolves to latest matching patch release`() {
    ReleaseTagResolver.resolveLatestMatchingRelease("v1.7.2", fakeReleases) shouldBe "v1.7.2.1"
  }

  @Test
  fun `commit SHA falls back to release-tag in action yaml`(@TempDir tempDir: Path) {
    writeActionYaml(tempDir, "v1.7.2")
    ReleaseTagResolver.resolve(
      "15ba5fa2b7eb9d9f2e67edb8cb355130b96d7a4d",
      tempDir,
      "VirtusLab/bazel-steward",
      fakeGh,
    ) shouldBe "v1.7.2"
  }

  @Test
  fun `branch ref falls back to release-tag in action yaml`(@TempDir tempDir: Path) {
    writeActionYaml(tempDir, "v1.7.2")
    ReleaseTagResolver.resolve("main", tempDir, "VirtusLab/bazel-steward", fakeGh) shouldBe "v1.7.2"
  }

  @Test
  fun `missing release-tag fails for non-v ref`(@TempDir tempDir: Path) {
    writeActionYamlWithoutReleaseTag(tempDir)
    shouldThrow<IllegalStateException> {
      ReleaseTagResolver.resolve("deadbeef", tempDir, "VirtusLab/bazel-steward", fakeGh)
    }
  }

  @Test
  fun `matchesTagPattern accepts optional numeric suffix segments`() {
    ReleaseTagResolver.matchesTagPattern("v1.7.2.1", "v1.7.2") shouldBe true
    ReleaseTagResolver.matchesTagPattern("v1.7.2-rc9", "v1.7.2") shouldBe false
    ReleaseTagResolver.matchesTagPattern("v1.7.3", "v1.7") shouldBe true
  }

  @Test
  fun `validateReleaseTag accepts stable and pre-release tags`() {
    ReleaseTagResolver.validateReleaseTag("v1.7.4") shouldBe "v1.7.4"
    ReleaseTagResolver.validateReleaseTag("v1.7.4-rc1") shouldBe "v1.7.4-rc1"
    ReleaseTagResolver.validateReleaseTag("v2.0.0-beta.2") shouldBe "v2.0.0-beta.2"
  }

  @Test
  fun `validateReleaseTag rejects malformed tags`() {
    shouldThrow<IllegalStateException> { ReleaseTagResolver.validateReleaseTag("") }
    shouldThrow<IllegalStateException> { ReleaseTagResolver.validateReleaseTag("1.7.4") }
    shouldThrow<IllegalStateException> { ReleaseTagResolver.validateReleaseTag("v1.7") }
    shouldThrow<IllegalStateException> { ReleaseTagResolver.validateReleaseTag("v1.7.2.1") }
  }

  @Test
  fun `readReleaseTagFromActionYaml accepts rc tag`(@TempDir tempDir: Path) {
    writeActionYaml(tempDir, "v1.7.4-rc1")
    ReleaseTagResolver.readReleaseTagFromActionYaml(tempDir) shouldBe "v1.7.4-rc1"
  }

  @Test
  fun `readReleaseTagFromActionYaml rejects invalid tag`(@TempDir tempDir: Path) {
    writeActionYaml(tempDir, "v1.7")
    shouldThrow<IllegalStateException> {
      ReleaseTagResolver.readReleaseTagFromActionYaml(tempDir)
    }
  }

  @Test
  fun `commit SHA falls back to rc release-tag in action yaml`(@TempDir tempDir: Path) {
    writeActionYaml(tempDir, "v1.7.4-rc1")
    ReleaseTagResolver.resolve(
      "15ba5fa2b7eb9d9f2e67edb8cb355130b96d7a4d",
      tempDir,
      "VirtusLab/bazel-steward",
      fakeGh,
    ) shouldBe "v1.7.4-rc1"
  }

  private fun writeActionYaml(dir: Path, releaseTag: String) {
    Files.writeString(
      dir.resolve("action.yaml"),
      """
      name: "Bazel Steward"
      release-tag: $releaseTag
      inputs: {}
      runs:
        using: composite
        steps: []
      """.trimIndent(),
    )
  }

  private fun writeActionYamlWithoutReleaseTag(dir: Path) {
    Files.writeString(
      dir.resolve("action.yaml"),
      """
      name: "Bazel Steward"
      inputs: {}
      runs:
        using: composite
        steps: []
      """.trimIndent(),
    )
  }
}
