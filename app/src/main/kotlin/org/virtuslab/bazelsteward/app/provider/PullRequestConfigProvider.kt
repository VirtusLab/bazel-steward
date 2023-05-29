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
  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveGroup(library: Library): GroupId? {
    return applier.forLibrary(library).findNotNullOrDefault(null) { it.groupId }
  }

  fun resolveForGroup(groupId: GroupId): PullRequestConfig {
    return resolveFromFilter(applier.forPredicate { it.groupId == groupId })
  }

  fun resolveForLibrary(library: Library): PullRequestConfig {
    return resolveFromFilter(applier.forLibrary(library))
  }

  private fun resolveFromFilter(filter: DependencyFilterApplier.Filtered<PullRequestsConfig>): PullRequestConfig {
    val title = filter.findNotNullOrDefault(default.titleTemplate) { it.title }
    val body = filter.findNotNullOrDefault(default.bodyTemplate) { it.body }
    val tags = filter.findNotNullOrDefault(default.labels) { it.labels }
    val prefix = filter.findNotNullOrDefault(default.branchPrefix) { it.branchPrefix }
    return PullRequestConfig(title, body, tags, prefix)
  }

  fun resolvePrefixes(): List<String> {
    val configsWithBranchPrefixes = configs.filter { it.branchPrefix != null }
    if (configsWithBranchPrefixes.isEmpty() && configs.all { it.acceptsAll() }) {
      return listOf(default.branchPrefix)
    }
    return configsWithBranchPrefixes.mapNotNull { it.branchPrefix }
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
