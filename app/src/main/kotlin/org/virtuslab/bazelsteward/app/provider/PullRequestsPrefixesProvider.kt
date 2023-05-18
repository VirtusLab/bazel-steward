package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.app.BazelStewardGitBranch
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig

class PullRequestsPrefixesProvider(private val configs: List<PullRequestsConfig>) {

  fun create(): List<String> {
    return configs.filter { it.prefix != null }
      .map { it.prefix!! }
      .plus(BazelStewardGitBranch.bazelPrefix)
  }
}