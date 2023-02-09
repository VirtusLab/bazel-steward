package org.virtuslab.bazelsteward.core.replacement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.*
import org.virtuslab.bazelsteward.maven.*
import kotlin.io.path.Path

class HeuristicTest {

  private fun getBazelFile(fileName: String): BazelFileSearch.BazelFile {
    val url = this::class.java.classLoader.getResource(fileName)!!
    return BazelFileSearch.BazelFileTest(Path(url.path), url.readText())
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SearchBuildFilesTest {

    @Test
    fun `should return right position offset`() {
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
      result[0].position shouldBe 2377
    }

    @Test
    fun `should return right position offset with wrong artifact`() {
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
      result[0].position shouldBe 2377
    }

    @Test
    fun `should return right position offset without artifact`() {
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
      result[0].position shouldBe 2377
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class WholeVersionHeuristicTest {

    @Test
    fun `should return right position WholeVersionHeuristic`() {
      val wholeVersionHeuristic = WholeVersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeVersionHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe 2377
    }

    @Test
    fun `should return right position WholeVersionHeuristic with wrong artifact`() {
      val wholeVersionHeuristic = WholeVersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeVersionHeuristic.apply(files, updateSuggestion)
      result shouldBe null
    }

    @Test
    fun `should return right position WholeVersionHeuristic without artifact`() {
      val wholeVersionHeuristic = WholeVersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("", ""),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = wholeVersionHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe 2377
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class VersionHeuristicTest {

    @Test
    fun `should return right position offset VersionHeuristic`() {
      val versionHeuristic = VersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.7theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe 2377
    }

    @Test
    fun `should return right position offset VersionHeuristic with wrong artifact`() {
      val versionHeuristic = VersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("com.10theta", "utilis"),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe 2377
    }

    @Test
    fun `should return right position offset VersionHeuristic without artifact`() {
      val versionHeuristic = VersionHeuristic()
      val lib = MavenCoordinates(
        MavenLibraryId("", ""),
        SemanticVersion(2, 3, 5, "", ""),
        VersioningSchema.SemVer,
        BumpingStrategy.Default
      )
      val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
      val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

      val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

      val result = versionHeuristic.apply(files, updateSuggestion)!!
      result.position shouldBe 2377
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

      val wholeVersionHeuristic = WholeVersionHeuristic()
      val result1 = wholeVersionHeuristic.apply(files, updateSuggestion)!!
      val versionHeuristic = VersionHeuristic()
      val result2 = versionHeuristic.apply(files, updateSuggestion)!!

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

      val wholeVersionHeuristic = WholeVersionHeuristic()
      val result1 = wholeVersionHeuristic.apply(files, updateSuggestion)
      val versionHeuristic = VersionHeuristic()
      val result2 = versionHeuristic.apply(files, updateSuggestion)!!

      result1 shouldBe null
      result2.position shouldBe 2377
    }
  }
}
