package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path
import kotlin.io.path.writeText

class MavenE2ETest : E2EBase() {

  @Test
  fun `Maven trivial local test`(@TempDir tempDir: Path) {
    val project = "maven/trivial"
    runBazelSteward(tempDir, project)
    val expectedBranches = expectedBranches(
      "io.arrow-kt/arrow-core" to "1.1.5",
      "io.arrow-kt/arrow-fx-coroutines" to "1.1.5",
      "bazel" to "5.4.0",
      "rules_jvm_external" to "4.5",
      "rules_kotlin" to "v1.7.1",
    )
    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }

  @Test
  fun `Check dependency update not in maven central repository`(@TempDir tempDir: Path) {
    val project = "maven/external"
    runBazelSteward(tempDir, project)
    val expectedBranches = expectedBranchPrefixes("com.7theta/utilis", "bazel", "rules_jvm_external", "rules_kotlin")
    checkBranchesWithoutVersions(tempDir, project, expectedBranches)
  }

  @Test
  fun `Test managing PRs when new version of library appears`(@TempDir tempDir: Path) {
    val project = "maven/updating-pr"
    val workspaceRoot = prepareWorkspace(tempDir, project)

    val gitHostClient = mockGitHostClientWithStatus(GitHostClient.PrStatus.NONE)
    val libraryName = "io.arrow-kt/arrow-core"

    val v1 = "1.1.0"
    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitHostClient).withMavenOnly(versions = listOf(v1))
    }

    val branchV1 = branch(libraryName, v1)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef))

    Assertions.assertThat(gitHostClient.openNewPrCalls[0].name).isEqualTo(branchV1.removePrefix(heads))
    Assertions.assertThat(gitHostClient.closeOldPrsCalls).isEmpty()

    val v2 = "1.1.3"
    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitHostClient).withMavenOnly(versions = listOf(v2))
    }

    val branchV2 = branch(libraryName, v2)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, branchV2, masterRef))

    Assertions.assertThat(gitHostClient.openNewPrCalls[1].name).isEqualTo(branchV2.removePrefix(heads))
    Assertions.assertThat(gitHostClient.closeOldPrsCalls[0].name).isEqualTo(branchV1.removePrefix(heads))
  }

  @Test
  fun `Test managing PRs when branch is no longer mergeable`(@TempDir tempDir: Path) {
    val project = "maven/updating-pr"
    val workspaceRoot = prepareWorkspace(tempDir, project)

    val localGit = GitClient(workspaceRoot)

    val v1 = "1.1.0"

    runBazelStewardWith(workspaceRoot) {
      it.withMavenOnly(versions = listOf(v1))
    }

    val branchV1 = branch("io.arrow-kt/arrow-core", v1)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef))

    val commitMessage = "Commit message 2137"
    val branchName = branchV1.removePrefix(heads)
    runBlocking {
      localGit.deleteBranch(branchName)
      val path = workspaceRoot.resolve("change.txt")
      path.writeText("This is a change")
      localGit.add(path)
      localGit.commit(commitMessage)
      localGit.push()
    }

    val gitHostClient = mockGitHostClientWithStatus(GitHostClient.PrStatus.OPEN_NOT_MERGEABLE)

    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitHostClient).withMavenOnly(versions = listOf(v1))
    }

    Assertions.assertThat(gitHostClient.openNewPrCalls).hasSize(0)
    Assertions.assertThat(gitHostClient.closeOldPrsCalls).hasSize(0)

    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef))

    runBlocking {
      localGit.checkout(branchName)
      val log = localGit.run("log", "--oneline")
      Assertions.assertThat(log).contains(commitMessage)
    }
  }

  @Test
  fun `Test managing PRs when branch is no longer mergeable but has been edited by user`(@TempDir tempDir: Path) {
    val project = "maven/updating-pr"
    val workspaceRoot = prepareWorkspace(tempDir, project)
    val localGit = GitClient(workspaceRoot)

    val v1 = "1.1.0"

    runBazelStewardWith(workspaceRoot) {
      it.withMavenOnly(versions = listOf(v1))
    }

    val branchV1 = branch("io.arrow-kt/arrow-core", v1)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef))

    val branchName = branchV1.removePrefix(heads)
    runBlocking {
      localGit.deleteBranch(branchName)
    }

    runBazelStewardWith(workspaceRoot) {
      it.withPRStatus(GitHostClient.PrStatus.OPEN_MODIFIED).withMavenOnly(versions = listOf(v1))
    }

    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef), skipLocal = true)
    checkBranchesWithVersions(tempDir, project, listOf(masterRef), skipRemote = true)
  }
}
