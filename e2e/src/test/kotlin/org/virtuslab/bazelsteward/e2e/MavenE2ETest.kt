package org.virtuslab.bazelsteward.e2e

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.Main
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.maven.MavenData
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.io.File

class MavenE2ETest : E2EBase() {

  @Test
  fun `Maven trivial local test`(@TempDir tempDir: File) {
    val testResourcePath = "maven/trivial"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches =
      listOf("arrow-core" to "1.1.5", "arrow-fx-coroutines" to "1.1.5", "bazel" to "5.3.2")
        .map { "$branchRef/${it.first}/${it.second}" } + masterRef
    checkBranchesWithVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Test
  fun `Check dependency update not in maven central repository`(@TempDir tempDir: File) {
    val testResourcePath = "maven/external"
    val file = loadTest(tempDir, testResourcePath)
    Main.main(args = arrayOf(file.toString(), "--push-to-remote"))
    val expectedBranches = listOf("$branchRef/utilis", "$branchRef/bazel/5.3.2", masterRef)
    checkBranchesWithoutVersions(tempDir, testResourcePath, expectedBranches)
  }

  @Test
  fun `Test managing PRs when new version appears`(@TempDir tempDir: File) {
    val testResourcePath = "maven/updating-pr"
    val file = loadTest(tempDir, testResourcePath)

    val gitHostClient =
      mockk<GitHostClient>(relaxed = true).also { every { it.checkPrStatus(any()) } returns GitHostClient.Companion.PrStatus.NONE }

    val v1 = "1.1.0"
    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mockMavenRepositoryWithVersion(v1), gitHostClient = gitHostClient)
    }

    checkBranchesWithVersions(tempDir, testResourcePath, listOf("$branchRef/arrow-core/$v1", masterRef))

    val v2 = "1.1.3"
    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mockMavenRepositoryWithVersion(v2), gitHostClient = gitHostClient)
    }

    verify(exactly = 2) { GitHostClient.stub.openNewPR(any()) }
    verify(exactly = 2) { GitHostClient.stub.closeOldPrs(any()) }

    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf("$branchRef/arrow-core/$v1", "$branchRef/arrow-core/$v2", masterRef)
    )
  }

  @Test
  fun `Test managing PRs when branch is no longer mergable`(@TempDir tempDir: File) {
    val testResourcePath = "maven/updating-pr"
    val file = loadTest(tempDir, testResourcePath)

    val v1 = "1.1.0"
    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mockMavenRepositoryWithVersion(v1))
    }

    checkBranchesWithVersions(tempDir, testResourcePath, listOf("$branchRef/arrow-core/$v1", masterRef))

    val gitHostClient =
      mockk<GitHostClient>(relaxed = true).also { every { it.checkPrStatus(any()) } returns GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE }

    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mockMavenRepositoryWithVersion(v1), gitHostClient = gitHostClient)
    }

    verifyOrder {
      GitHostClient.stub.openNewPR(any())
      GitHostClient.stub.closeOldPrs(any())
      GitHostClient.stub.openNewPR(any())
      GitHostClient.stub.closeOldPrs(any())
    }

    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf("$branchRef/arrow-core/$v1", masterRef)
    )
  }

  private fun mockMavenRepositoryWithVersion(version: String): MavenRepository {
    return mockk<MavenRepository>().also {
      val slot = slot<MavenData>()
      coEvery { it.findVersions(capture(slot)) } answers {
        mapOf(
          slot.captured.dependencies[0] to listOfNotNull(
            SemanticVersion.fromString(version)
          )
        )
      }
    }
  }
}
