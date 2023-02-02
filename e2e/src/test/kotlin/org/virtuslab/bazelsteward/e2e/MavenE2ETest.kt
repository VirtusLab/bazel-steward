package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.Main
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.GitClient
import java.io.File
import java.nio.file.Files

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
  fun `Test managing PRs when new version of library appears`(@TempDir tempDir: File) {
    val testResourcePath = "maven/updating-pr"
    val file = loadTest(tempDir, testResourcePath)

    val gitHostClient = mockGitHostClientWithStatus(GitHostClient.Companion.PrStatus.NONE)
    val bazelUpdater = mockBazelUpdaterWithVersion()
    val v1 = "1.1.0"
    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(
        mavenRepository = mockMavenRepositoryWithVersion(v1),
        gitHostClient = gitHostClient,
        bazelUpdater = bazelUpdater
      )
    }

    val branchV1 = "$branchRef/arrow-core/$v1"
    checkBranchesWithVersions(tempDir, testResourcePath, listOf(branchV1, masterRef))

    Assertions.assertThat(gitHostClient.openNewPrCalls[0].name).isEqualTo(branchV1.removePrefix(heads))
    Assertions.assertThat(gitHostClient.closeOldPrsCalls[0].name).isEqualTo(branchV1.removePrefix(heads))

    val v2 = "1.1.3"
    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(
        mavenRepository = mockMavenRepositoryWithVersion(v2),
        gitHostClient = gitHostClient,
        bazelUpdater = bazelUpdater
      )
    }

    val branchV2 = "$branchRef/arrow-core/$v2"
    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf(branchV1, branchV2, masterRef)
    )

    Assertions.assertThat(gitHostClient.openNewPrCalls[1].name).isEqualTo(branchV2.removePrefix(heads))
    Assertions.assertThat(gitHostClient.closeOldPrsCalls[1].name).isEqualTo(branchV2.removePrefix(heads))
  }

  @Test
  fun `Test managing PRs when branch is no longer mergeable`(@TempDir tempDir: File) {
    val testResourcePath = "maven/updating-pr"
    val file = loadTest(tempDir, testResourcePath)
    val localGit = GitClient(file)

    val v1 = "1.1.0"
    val mavenRepository = mockMavenRepositoryWithVersion(v1)
    val bazelUpdater = mockBazelUpdaterWithVersion()

    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mavenRepository, bazelUpdater = bazelUpdater)
    }

    val branchV1 = "$branchRef/arrow-core/$v1"
    checkBranchesWithVersions(tempDir, testResourcePath, listOf(branchV1, masterRef))

    val commitMessage = "Commit message 2137"
    val branchName = branchV1.removePrefix(heads)
    runBlocking {
      localGit.runGitCommand("branch -D $branchName".split(' '))

      val path = file.toPath().resolve("change.txt")
      withContext(Dispatchers.IO) {
        Files.writeString(path, "This is a change")
      }
      localGit.add(path)
      localGit.commit(commitMessage)
      localGit.push()
    }

    val gitHostClient = mockGitHostClientWithStatus(GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE)

    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mavenRepository, gitHostClient = gitHostClient, bazelUpdater = bazelUpdater)
    }

    Assertions.assertThat(gitHostClient.openNewPrCalls).hasSize(0)
    Assertions.assertThat(gitHostClient.closeOldPrsCalls).hasSize(0)

    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf(branchV1, masterRef)
    )

    runBlocking {
      localGit.checkout(branchName)
      val log = localGit.runGitCommand("log", "--oneline")
      Assertions.assertThat(log).contains(commitMessage)
    }
  }

  @Test
  fun `Test managing PRs when branch is no longer mergeable but has been edited by user`(@TempDir tempDir: File) {
    val testResourcePath = "maven/updating-pr"
    val file = loadTest(tempDir, testResourcePath)
    val localGit = GitClient(file)

    val v1 = "1.1.0"
    val mavenRepository = mockMavenRepositoryWithVersion(v1)
    val bazelUpdater = mockBazelUpdaterWithVersion()

    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(mavenRepository = mavenRepository, bazelUpdater = bazelUpdater)
    }

    val branchV1 = "$branchRef/arrow-core/$v1"
    checkBranchesWithVersions(tempDir, testResourcePath, listOf(branchV1, masterRef))

    val branchName = branchV1.removePrefix(heads)
    runBlocking {
      localGit.runGitCommand("branch", "-D", branchName)
    }

    Main.mainMapContext(arrayOf(file.toString(), "--push-to-remote")) {
      it.copy(
        mavenRepository = mavenRepository,
        gitHostClient = mockGitHostClientWithStatus(GitHostClient.Companion.PrStatus.OPEN_MODIFIED),
        bazelUpdater = bazelUpdater
      )
    }

    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf(branchV1, masterRef),
      skipLocal = true,
    )

    checkBranchesWithVersions(
      tempDir,
      testResourcePath,
      listOf(masterRef),
      skipRemote = true,
    )
  }
}
