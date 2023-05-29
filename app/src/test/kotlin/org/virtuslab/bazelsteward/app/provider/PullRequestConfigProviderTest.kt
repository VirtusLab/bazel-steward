package org.virtuslab.bazelsteward.app.provider

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.config.repo.DependencyNameFilter
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig

class PullRequestConfigProviderTest {
  @Test
  fun `should correctly resolve prefixes from configs`() {
    val config1 = PullRequestsConfig(
      kinds = listOf("maven"),
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
      branchPrefix = "test-prefix-1/",
    )

    val config2 = PullRequestsConfig(
      dependencies = listOf(DependencyNameFilter.Default("commons-io:commons-io")),
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
      branchPrefix = "test-prefix-2/",
    )

    val provider = PullRequestConfigProvider(listOf(config1, config2), emptyList())

    val resolvedPrefixes = provider.resolvePrefixes()
    resolvedPrefixes shouldBe listOf("test-prefix-1/", "test-prefix-2/")
  }

  @Test
  fun `should correctly resolve default prefix`() {
    val config = PullRequestsConfig(
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
    )

    val provider = PullRequestConfigProvider(listOf(config), emptyList())

    val resolvedPrefixes = provider.resolvePrefixes()
    resolvedPrefixes shouldBe listOf("bazel-steward/")
  }
}
