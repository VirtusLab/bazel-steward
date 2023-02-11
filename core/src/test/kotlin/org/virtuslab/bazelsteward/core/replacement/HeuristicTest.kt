package org.virtuslab.bazelsteward.core.replacement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Path
import kotlin.io.path.Path

class HeuristicTest {

  val correctPositionFor235: Int = 2401
  val correctPositionFor120: Int = 2263
  val correctPositionFor160: Int = 2464

  data class TestBazelFile(override val path: Path, override val content: String) : BazelFileSearch.BazelFile

  private fun getBazelFile(fileName: String): BazelFileSearch.BazelFile {
    val url = this::class.java.classLoader.getResource(fileName)!!
    return TestBazelFile(Path(url.path), url.readText())
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SearchBuildFilesTest {

    @Test
    fun `should return correct position offset`() {
      val fileUpdateSearch = FileUpdateSearch()
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result: List<FileUpdateSearch.FileChangeSuggestion> =
        fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
      result[0].position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset with wrong artifact`() {
      val fileUpdateSearch = FileUpdateSearch()
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result: List<FileUpdateSearch.FileChangeSuggestion> =
        fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
      result[0].position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset without artifact`() {
      val fileUpdateSearch = FileUpdateSearch()
      val lib = MavenCoordinates(
        MavenLibraryId("", ""),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result: List<FileUpdateSearch.FileChangeSuggestion> =
        fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
      result[0].position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset for artifact with version in variable`() {
      val fileUpdateSearch = FileUpdateSearch()
      val lib = MavenCoordinates(
        MavenLibraryId("io.grpc", "grpc-core"),
        SemanticVersion(1, 2, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result: List<FileUpdateSearch.FileChangeSuggestion> =
        fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
      result[0].position shouldBe correctPositionFor120
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val fileUpdateSearch = FileUpdateSearch()
      val lib = MavenCoordinates(
        MavenLibraryId("org.jetbrains.kotlinx", "kotlinx-coroutines-core"),
        SemanticVersion(1, 6, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result: List<FileUpdateSearch.FileChangeSuggestion> =
        fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
      result[0].position shouldBe correctPositionFor160
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class WholeLibraryHeuristicTest {

    @Test
    fun `should return correct position`() {
      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeLibraryHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor235
    }

    @Test
    fun `should return null with wrong artifact`() {
      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeLibraryHeuristic.apply(files, updateSuggestion)
      result shouldBe null
    }

    @Test
    fun `should return correct position without artifact`() {
      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("", ""),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeLibraryHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor235
    }

    @Test
    fun `should return null for artifact with version in variable`() {
      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("io.grpc", "grpc-core"),
        SemanticVersion(1, 2, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeLibraryHeuristic.apply(files, updateSuggestion)
      result shouldBe null
    }

    @Test
    fun `should return correct position offset when two libraries have same version`() {
      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("org.jetbrains.kotlinx", "kotlinx-coroutines-core"),
        SemanticVersion(1, 6, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeLibraryHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor160
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class VersionOnlyHeuristicTest {

    @Test
    fun `should return correct position offset `() {
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionOnlyHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset with wrong artifact`() {
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionOnlyHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position offset without artifact`() {
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("", ""),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionOnlyHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor235
    }

    @Test
    fun `should return correct position for artifact with version in variable`() {
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("io.grpc", "grpc-core"),
        SemanticVersion(1, 2, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionOnlyHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe correctPositionFor120
    }

    @Test
    fun `should return null when two libraries have same version`() {
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("org.jetbrains.kotlinx", "kotlinx-coroutines-core"),
        SemanticVersion(1, 6, 0, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionOnlyHeuristic.apply(files, updateSuggestion)
      result shouldBe null
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class CompareHeuristicTest {

    @Test
    fun `should return same position offset for WholeVersionHeuristic and VersionHeuristic`() {
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val result1 = wholeLibraryHeuristic.apply(files, updateSuggestion)!!
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val result2 = versionOnlyHeuristic.apply(files, updateSuggestion)!!

      result1.position shouldBe result2.position
    }

    @Test
    fun `should return different position offset for WholeVersionHeuristic and VersionHeuristic`() {
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val wholeLibraryHeuristic = WholeLibraryHeuristic()
      val result1 = wholeLibraryHeuristic.apply(files, updateSuggestion)
      val versionOnlyHeuristic = VersionOnlyHeuristic()
      val result2 = versionOnlyHeuristic.apply(files, updateSuggestion)!!

      result1 shouldBe null
      result2.position shouldBe correctPositionFor235
    }
  }
}
