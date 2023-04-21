package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.core.GitPlatform
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import java.nio.file.Path
import kotlin.io.path.writeText

class PrManagementTest : E2EBase() {

  @Test
  fun `Test managing PRs when new version of library appears`(@TempDir tempDir: Path) {
    val project = "maven/updating-pr"
    val workspaceRoot = prepareWorkspace(tempDir, project)

    val gitPlatform = mockGitHostClientWithStatus(GitPlatform.PrStatus.NONE)
    val libraryName = "io.arrow-kt/arrow-core"

    val v1 = "1.1.0"
    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitPlatform).withMavenOnly(versions = listOf(v1))
    }

    val branchV1 = branch(libraryName, v1)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef))

    Assertions.assertThat(gitPlatform.openNewPrCalls[0].name).isEqualTo(branchV1.removePrefix(heads))
    Assertions.assertThat(gitPlatform.closeOldPrsCalls).isEmpty()

    val v2 = "1.1.3"
    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitPlatform).withMavenOnly(versions = listOf(v2))
    }

    val branchV2 = branch(libraryName, v2)
    checkBranchesWithVersions(tempDir, project, listOf(branchV1, branchV2, masterRef))

    Assertions.assertThat(gitPlatform.openNewPrCalls[1].name).isEqualTo(branchV2.removePrefix(heads))
    Assertions.assertThat(gitPlatform.closeOldPrsCalls[0].name).isEqualTo(branchV1.removePrefix(heads))
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

    val gitPlatform = mockGitHostClientWithStatus(GitPlatform.PrStatus.OPEN_NOT_MERGEABLE)

    runBazelStewardWith(workspaceRoot) {
      it.withGitHostClient(gitPlatform).withMavenOnly(versions = listOf(v1))
    }

    Assertions.assertThat(gitPlatform.openNewPrCalls).hasSize(0)
    Assertions.assertThat(gitPlatform.closeOldPrsCalls).hasSize(0)

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
      it.withPRStatus(GitPlatform.PrStatus.OPEN_MODIFIED).withMavenOnly(versions = listOf(v1))
    }

    checkBranchesWithVersions(tempDir, project, listOf(branchV1, masterRef), skipLocal = true)
    checkBranchesWithVersions(tempDir, project, listOf(masterRef), skipRemote = true)
  }
}
