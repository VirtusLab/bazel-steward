package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig

class PullRequestsPrefixesProvider(private val configs: List<PullRequestsConfig>) {

  fun create(): List<String> {
    return configs.map { it.prefix!! }
  }
}