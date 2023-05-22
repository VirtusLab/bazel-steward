package org.virtuslab.bazelsteward.app.provider

import mu.KotlinLogging
import org.virtuslab.bazelsteward.app.PullRequestsLimits
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.GitPlatform

private val logger = KotlinLogging.logger {}

class PullRequestsLimitsProvider(
  private val configs: List<PullRequestsConfig>,
  private val gitPlatform: GitPlatform,
  private val updateAllPullRequests: Boolean,
  private val pullRequestsConfigProvider: PullRequestConfigProvider
) {
  fun create(): PullRequestsLimits {
    val configsWithLimits = configs.filter { it.limits != null }
    val (correct, incorrect) = configsWithLimits.partition { it.acceptsAll() }

    if (incorrect.isNotEmpty()) {
      logger.warn {
        "Pull Request limits with dependency filters are not supported. " +
          "Please don't use 'dependencies' or 'kinds' fields for pull request " +
          "configuration that includes limits." +
          "Incorrect configs: $incorrect"
      }
    }

    val maxOpenPrs = correct.mapNotNull { it.limits?.maxOpen }.maxOrNull()
    val maxUpdates = correct.mapNotNull { it.limits?.maxUpdatesPerRun }.maxOrNull()

    val prefixes = pullRequestsConfigProvider.resolvePrefixes()

    val openPrs = maxOpenPrs?.let {
      gitPlatform.getOpenPrs()
      .count { pr -> prefixes.any { pr.branch.name.startsWith(it) } }
    } ?: 0

    return PullRequestsLimits(openPrs, maxOpenPrs, maxUpdates, updateAllPullRequests)
  }
}
