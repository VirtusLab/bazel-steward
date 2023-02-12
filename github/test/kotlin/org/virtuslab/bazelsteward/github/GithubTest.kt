package org.virtuslab.bazelsteward.github

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient.PrStatus
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SimpleVersion

/**
 * You need to set four env vars to run this test suite:
 * GITHUB_ACTIONS=true;
 * GITHUB_API_URL=https://api.github.com;
 * GITHUB_REPOSITORY=VirtusLab/bazel-steward;
 * GITHUB_TOKEN=[your token]
 */

@EnabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class GithubTest {

  @Test
  fun `Check if PR statuses are detected correctly`() {
    val branchToPrStatus = listOf(
      "bazel-steward/testing/closed" to PrStatus.CLOSED,
      "bazel-steward/testing/mergable" to PrStatus.OPEN_MERGEABLE,
      "bazel-steward/testing/merged" to PrStatus.MERGED,
      "bazel-steward/testing/modified" to PrStatus.OPEN_MODIFIED,
      "bazel-steward/testing/not_mergeable" to PrStatus.OPEN_NOT_MERGEABLE,
      "bazel-steward/testing/none" to PrStatus.NONE
    )

    val env = Environment.system
    val gitAuthor = GitClient.GitAuthor("github-actions[bot]", "email@github.com")
    val gitHostClient = GithubClient.getClient(env, baseBranch = "base", gitAuthor)

    branchToPrStatus.forEach {
      val branch = simpleBranch(it.first)
      val status = gitHostClient.checkPrStatus(branch)
      Assertions.assertEquals(status, it.second)
    }
  }

  private fun simpleBranch(branch: String): GitBranch {
    val split = branch.split('/', limit = 3)
    val lib = object : LibraryId() {
      override fun associatedStrings(): List<String> = emptyList()
      override val name = split[1]
    }
    val ver = SimpleVersion(split[2])

    return GitBranch(lib.name + ver.value)
  }
}
