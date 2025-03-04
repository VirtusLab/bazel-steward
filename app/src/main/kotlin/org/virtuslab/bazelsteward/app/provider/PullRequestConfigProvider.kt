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
    val commitMessage = filter.findNotNullOrDefault(default.commitMessageTemplate) { it.commitMessage }
    val tags = filter.findNotNullOrDefault(default.labels) { it.labels }
    val branchPrefix = filter.findNotNullOrDefault(default.branchPrefix) { it.branchPrefix }
    return PullRequestConfig(title, body, commitMessage, tags, branchPrefix)
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
      titleTemplate = "Updated \${dependencyId} to \${versionTo}",
      bodyTemplate = "Updates \${dependencyId} from \${versionFrom} to \${versionTo}",
      commitMessageTemplate = "Update \${dependencyId} to \${versionTo}",
      labels = emptyList(),
      branchPrefix = "bazel-steward/",
    )
  }
}

data class PullRequestConfig(
  val titleTemplate: String,
  val bodyTemplate: String,
  val commitMessageTemplate: String,
  val labels: List<String>,
  val branchPrefix: String,
)
