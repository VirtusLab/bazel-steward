package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.library.Library

open class PullRequestConfigProvider(
  configs: List<PullRequestsConfig>,
  dependencyKinds: List<DependencyKind<*>>
) {
  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  open fun resolveForLibrary(library: Library): PullRequestConfig {
    val filter = applier.forLibrary(library)
    val title = filter.findNotNull { it.title }?.title ?: defaultPullRequestConfig.titleTemplate
    val body = filter.findNotNull { it.body }?.body ?: defaultPullRequestConfig.bodyTemplate
    val tags = filter.findNotNull { it.labels }?.labels ?: defaultPullRequestConfig.labels
    return PullRequestConfig(title, body, tags)
  }

  companion object {
    val defaultPullRequestConfig = PullRequestConfig(
      "Updated \${dependencyId} to \${versionTo}",
      "Updates \${dependencyId} from \${versionFrom} to \${versionTo}",
      emptyList()
    )
  }
}
