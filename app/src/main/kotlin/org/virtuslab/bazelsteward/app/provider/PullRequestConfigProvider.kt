package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.app.DependencyFilterApplier
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.library.GroupId
import org.virtuslab.bazelsteward.core.library.Library

class PullRequestConfigProvider(
  private val configs: List<PullRequestsConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {
  private val filter = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveGroup(library: Library): GroupId? {
    return filter.forLibrary(library).findNotNullOrDefault(null) { it.groupId }
  }

  fun resolveForGroup(groupId: GroupId): PullRequestConfig {
    return resolveFromFilter(filter.forPredicate { it.groupId == groupId })
  }

  fun resolveForLibrary(library: Library): PullRequestConfig {
    return resolveFromFilter(filter.forLibrary(library))
  }

  private fun resolveFromFilter(filter: DependencyFilterApplier.Filtered<PullRequestsConfig>): PullRequestConfig {
    val title = filter.findNotNullOrDefault(default.titleTemplate) { it.title }
    val body = filter.findNotNullOrDefault(default.bodyTemplate) { it.body }
    val tags = filter.findNotNullOrDefault(default.labels) { it.labels }
    val branchPrefix = filter.findNotNullOrDefault(default.branchPrefix) { it.branchPrefix }
    return PullRequestConfig(title, body, tags, branchPrefix)
  }

  fun resolveBranchPrefixes(): List<String> {
    val branchPrefixConfiguredGlobally = configs.any { it.acceptsAll() && it.branchPrefix != null }
    val configsWithBranchPrefixes = configs.mapNotNull { it.branchPrefix }
    return if (branchPrefixConfiguredGlobally) {
      configsWithBranchPrefixes
    } else {
      configsWithBranchPrefixes.plus(default.branchPrefix)
    }
  }

  companion object {
    val default = PullRequestConfig(
      "Updated \${dependencyId} to \${versionTo}",
      "Updates \${dependencyId} from \${versionFrom} to \${versionTo}",
      emptyList(),
      "bazel-steward/",
    )
  }
}

data class PullRequestConfig(
  val titleTemplate: String,
  val bodyTemplate: String,
  val labels: List<String>,
  val branchPrefix: String,
)
