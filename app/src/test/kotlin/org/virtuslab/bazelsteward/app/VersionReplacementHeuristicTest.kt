package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import org.virtuslab.bazelsteward.core.replacement.PythonFunctionCallHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionOnlyHeuristic
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic
import org.virtuslab.bazelsteward.core.replacement.WholeLibraryHeuristic
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Path
import kotlin.io.path.Path

class VersionReplacementHeuristicTest {

  val correctPositionFor235: Int = 2401
  val correctPositionFor120: Int = 2263
  val correctPositionFor160: Int = 2464
  val correctPositionFor3200jre: Int = 2764
  val correctPositionFor113: Int = 2935
  val correctPositionFor3212: Int = 3128
  val correctPositionFor4132: Int = 3058
  val correctPositionFor852: Int = 3326

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SearchBuildFilesTest {

    @Test
    fun `should return correct position offset`() {
      val library = library("com.7theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset with wrong artifact`() {
      val library = library("com.10theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset without artifact`() {
      val library = library("", "", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion)

      result?.offset shouldBe correctPositionFor120
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val library = library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion)

      result?.offset shouldBe correctPositionFor160
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class WholeLibraryHeuristicTest {

    @Test
    fun `should return correct position`() {
      val library = library("com.7theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return null with wrong artifact`() {
      val library = library("com.10theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)

      result shouldBe null
    }

    @Test
    fun `should return correct position without artifact`() {
      val library = library("", "", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return null for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)

      result shouldBe null
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val library = library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)

      result?.offset shouldBe correctPositionFor160
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class VersionOnlyHeuristicTest {

    @Test
    fun `should return correct position offset `() {
      val library = library("com.7theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset with wrong artifact`() {
      val library = library("com.10theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset without artifact`() {
      val library = library("", "", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result?.offset shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position for artifact with version in variable`() {
      val library = library("io.grpc", "grpc-core", "1.2.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result?.offset shouldBe correctPositionFor120
    }

    @Test
    fun `should return null when two libraries have same version`() {
      val library = library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
      val suggestedVersion = version("2.4.0")

      val result = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result shouldBe null
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class PythonFunctionCallHeuristicTest {

    @Test
    fun `should return correct position offset maven artifact`() {
      val library = library("com.google.guava", "guava-testlib", "31.1.0-jre")
      val suggestedVersion = version("32.0.0-jre")

      val result = resolveUpdates(library, suggestedVersion, PythonFunctionCallHeuristic)

      result?.offset shouldBe correctPositionFor3200jre
    }

    @Test
    fun `should return correct position offset maven artifact named parameters`() {
      val library = library("com.google.truth", "truth", "1.1.3")
      val suggestedVersion = version("1.2.0")

      val result = resolveUpdates(library, suggestedVersion, PythonFunctionCallHeuristic)

      result?.offset shouldBe correctPositionFor113
    }

    @Test
    fun `should return correct position offset scala dep`() {
      val library = library("org.scalactic", "scalactic", "3.2.12")
      val suggestedVersion = version("4.0.0")

      val result = resolveUpdates(library, suggestedVersion, PythonFunctionCallHeuristic)

      result?.offset shouldBe correctPositionFor3212
    }

    @Test
    fun `should return correct position offset scala dep named parameters`() {
      val library = library("junit", "junit", "4.13.2")
      val suggestedVersion = version("4.14.0")

      val result = resolveUpdates(library, suggestedVersion, PythonFunctionCallHeuristic)

      result?.offset shouldBe correctPositionFor4132
    }

    @Test
    fun `should return correct position offset scala dep with scala version`() {
      val library = library("com.sksamuel.elastic4s", "elastic4s-client-akka_2.12", "8.5.2")
      val suggestedVersion = version("8.6.0")

      val result = resolveUpdates(library, suggestedVersion, PythonFunctionCallHeuristic)

      result?.offset shouldBe correctPositionFor852
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class CompareHeuristicTest {

    @Test
    fun `should return same position offset for WholeVersionHeuristic and VersionHeuristic`() {
      val library = library("com.7theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result1 = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)
      val result2 = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result1?.offset shouldBe result2?.offset
    }

    @Test
    fun `should return different position offset for WholeVersionHeuristic and VersionHeuristic`() {
      val library = library("com.10theta", "utilis", "2.3.5")
      val suggestedVersion = version("2.4.0")

      val result1 = resolveUpdates(library, suggestedVersion, WholeLibraryHeuristic)
      val result2 = resolveUpdates(library, suggestedVersion, VersionOnlyHeuristic)

      result1 shouldBe null
      result2?.offset shouldBe correctPositionFor235
    }
  }

  private data class TestTextFile(override val path: Path, override val content: String) : TextFile

  private fun loadTextFileFromResources(fileName: String): TextFile {
    val url = this::class.java.classLoader.getResource(fileName)!!
    return TestTextFile(Path(url.path), url.readText())
  }

  private fun library(group: String, artifact: String, version: String) =
    MavenCoordinates(
      MavenLibraryId(group, artifact),
      version(version)
    )

  private fun version(version: String) = SemanticVersion.fromString(version)!!

  private val files = listOf(loadTextFileFromResources("WORKSPACE.bzlignore"))

  private val resolver = LibraryUpdateResolver()

  private val allHeuristics = listOf(WholeLibraryHeuristic, VersionOnlyHeuristic, PythonFunctionCallHeuristic).toTypedArray()

  private fun resolveUpdates(
    library: MavenCoordinates,
    version: SemanticVersion,
    vararg heuristics: VersionReplacementHeuristic = allHeuristics
  ): FileChange? {
    return resolver.resolve(files, UpdateSuggestion(library, version), heuristics.toList())?.fileChanges?.firstOrNull()
  }
}
