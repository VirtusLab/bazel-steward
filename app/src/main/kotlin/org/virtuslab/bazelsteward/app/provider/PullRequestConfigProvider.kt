package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.app.DependencyFilterApplier
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.library.Library

open class PullRequestConfigProvider(
  configs: List<PullRequestsConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {
  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  open fun resolveForLibrary(library: Library): PullRequestConfig {
    val filter = applier.forLibrary(library)
    val title = filter.findNotNullOrDefault(default.titleTemplate) { it.title }
    val body = filter.findNotNullOrDefault(default.bodyTemplate) { it.body }
    val tags = filter.findNotNullOrDefault(default.labels) { it.labels }
    return PullRequestConfig(title, body, tags)
  }

  companion object {
    val default = PullRequestConfig(
      "Updated \${dependencyId} to \${versionTo}",
      "Updates \${dependencyId} from \${versionFrom} to \${versionTo}",
      emptyList(),
    )
  }
}

data class PullRequestConfig(
  val titleTemplate: String,
  val bodyTemplate: String,
  val labels: List<String>,
)
