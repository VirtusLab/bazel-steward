package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import org.virtuslab.bazelsteward.core.replacement.PythonFunctionCallHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyInStringLiteralHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import org.virtuslab.bazelsteward.fixture.loadTextFileFromResources
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId

class VersionReplacementHeuristicTest {

  val positionOf235: Int = 2401
  val positionOf120: Int = 2263
  val positionOf160: Int = 2464
  val positionOf3200jre: Int = 2764
  val positionOf113: Int = 2935
  val positionOf3212: Int = 3128
  val positionOf4132: Int = 3058
  val positionOf852: Int = 3326
  val positionOfJunitJupiter581: Int = 3432
  val positionOf1_99_99: Int = 3526

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SearchBuildFilesTest {

    @Test
    fun `should return correct position offset`() {
      val library = library("com.7theta", "utilis", "2.3.5")

      val result = resolveUpdates(library)

      result?.offset shouldBe positionOf235
    }

    @Test
    fun `should not return correct position offset with wrong artifact`() {
      val library = library("com.10theta", "utilis", "2.3.5")

      val result = resolveUpdates(library)

      result?.offset shouldBe null
    }

    @Test
    fun `should return correct position offset for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")

      val result = resolveUpdates(library)

      result?.offset shouldBe positionOf120
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val library = library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")

      val result = resolveUpdates(library)

      result?.offset shouldBe positionOf160
    }

    @Test
    fun `should not find position of library that is not defined in available project sources despite existing version string`() {
      val library = library("com.example", "not-existent", "1.99.99")

      val result = resolveUpdates(library)

      result?.offset shouldBe null
    }

    @Test
    fun `should ignore commented code`() {
      val library = library("org.virtuslab", "bazel-steward", "1.99.99")

      val result = resolveUpdates(library)

      result?.offset shouldBe positionOf1_99_99
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class WholeLibraryHeuristicTest {

    @Test
    fun `should return correct position`() {
      val library = library("com.7theta", "utilis", "2.3.5")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result?.offset shouldBe positionOf235
    }

    @Test
    fun `should return null with wrong artifact`() {
      val library = library("com.10theta", "utilis", "2.3.5")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result shouldBe null
    }

    @Test
    fun `should return correct position without artifact`() {
      val library = library("", "", "2.3.5")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result?.offset shouldBe positionOf235
    }

    @Test
    fun `should return null for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result shouldBe null
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val library = library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result?.offset shouldBe positionOf160
    }

    @Test
    fun `should return correct position offset when two libraries have same version and one is prefix of another`() {
      val library = library("org.junit.jupiter", "junit-jupiter", "5.8.1")

      val result = resolveUpdates(library, WholeLibraryHeuristic)

      result?.offset shouldBe positionOfJunitJupiter581
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class VersionOnlyHeuristicTest {

    @Test
    fun `should return correct position for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")

      val result = resolveUpdates(library, VersionOnlyInStringLiteralHeuristic)

      result?.offset shouldBe positionOf120
    }

    @Test
    fun `should return null when two libraries have same version`() {
      val library = library("com.example", "test", "9.9.9")

      val result = resolveUpdates(library, VersionOnlyInStringLiteralHeuristic)

      result shouldBe null
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class PythonFunctionCallHeuristicTest {

    @Test
    fun `should return correct position offset maven artifact`() {
      val library = library("com.google.guava", "guava-testlib", "31.1.0-jre")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result?.offset shouldBe positionOf3200jre
    }

    @Test
    fun `should return correct position offset maven artifact named parameters`() {
      val library = library("com.google.truth", "truth", "1.1.3")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result?.offset shouldBe positionOf113
    }

    @Test
    fun `should return correct position offset scala dep`() {
      val library = library("org.scalactic", "scalactic", "3.2.12")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result?.offset shouldBe positionOf3212
    }

    @Test
    fun `should return null for wrong scala dep`() {
      val library = library("org.scalactic", "scalactic", "3.2.90")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result shouldBe null
    }

    @Test
    fun `should return correct position offset scala dep named parameters`() {
      val library = library("junit", "junit", "4.13.2")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result?.offset shouldBe positionOf4132
    }

    @Test
    fun `should return correct position offset scala dep with scala version`() {
      val library = library("com.sksamuel.elastic4s", "elastic4s-client-akka_2.12", "8.5.2")

      val result = resolveUpdates(library, PythonFunctionCallHeuristic)

      result?.offset shouldBe positionOf852
    }
  }

  private fun library(group: String, artifact: String, version: String) =
    MavenCoordinates(
      MavenLibraryId(group, artifact),
      version(version),
    )

  private fun version(version: String) = SemanticVersion.fromString(version)!!

  private val files = listOf(loadTextFileFromResources("WORKSPACE.bzlignore"))

  private val resolver = LibraryUpdateResolver()

  private val allHeuristics = listOf(WholeLibraryHeuristic, PythonFunctionCallHeuristic, VersionOnlyInStringLiteralHeuristic).toTypedArray()

  private fun resolveUpdates(
    library: MavenCoordinates,
    vararg heuristics: VersionReplacementHeuristic = allHeuristics,
  ): FileChange? {
    return resolver.resolve(files, UpdateSuggestion(library, version("999.999.999")), heuristics.toList())?.fileChanges?.firstOrNull()
  }
}
