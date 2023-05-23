package org.virtuslab.bazelsteward.app.provider
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PullRequestConfigProviderTest {
  @Test
  fun `should correctly resolve prefixes from configs`() {
    val config = PullRequestsConfig(
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
      branchPrefix = "test-prefix"
    )

    val provider = PullRequestConfigProvider(listOf(config), emptyList())

    val resolvedPrefixes = provider.resolvePrefixes()
    resolvedPrefixes shouldBe listOf("test-prefix", "bazel-steward")
  }
}