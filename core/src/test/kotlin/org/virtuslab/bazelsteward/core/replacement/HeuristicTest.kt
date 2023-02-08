package org.virtuslab.bazelsteward.core.replacement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.maven.*
import org.virtuslab.bazelsteward.core.library.*
import kotlin.io.path.Path

class HeuristicTest {

  private fun getBazelFile(fileName: String): BazelFileSearch.BazelFile{
    val url = this::class.java.classLoader.getResource(fileName)!!
    return BazelFileSearch.BazelFileTest(Path(url.path), url.readText())
  }

  @Test
  fun `should return right position offset`() {
    val fileUpdateSearch = FileUpdateSearch()
    val lib = MavenCoordinates(
      MavenLibraryId("com.7theta", "utilis"),
      SemanticVersion(2, 3, 5, "", ""),
      VersioningSchema.SemVer,
      BumpingStrategy.Default)
    val suggestedVersion = SemanticVersion(2, 4, 0, "", "")
    val updateSuggestion = UpdateLogic.UpdateSuggestion(lib, suggestedVersion)

    val files = listOf(getBazelFile("WORKSPACE.bzlignore"))

    val result: List<FileUpdateSearch.FileChangeSuggestion> = fileUpdateSearch.searchBuildFiles(files, listOf(updateSuggestion))
    result[0].position shouldBe 2377

  }
}