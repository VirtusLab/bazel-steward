package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.app.DependencyFilterApplier
import org.virtuslab.bazelsteward.config.repo.PostUpdateHooksConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.common.HookRunFor
import org.virtuslab.bazelsteward.core.library.Library

class PostUpdateHookProvider(
  configs: List<PostUpdateHooksConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {
  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveForLibrary(library: Library): PostUpdateHookConfig {
    val filter = applier.forLibrary(library)
    val commands = filter.findNotNullOrDefault(default.commands) { it.commands }
    val filesToCommit = filter.findNotNullOrDefault(default.filesToCommit) { it.filesToCommit }
    val runFor = filter.findNotNullOrDefault(default.runFor) { it.runFor }
    val commitMessage = filter.findNotNullOrDefault(default.commitMessage) { it.commitMessage }
    return PostUpdateHookConfig(commands, filesToCommit, runFor, commitMessage)
  }

  companion object {
    val default = PostUpdateHookConfig(
      emptyList(),
      emptyList(),
      HookRunFor.Commit,
      "Post update hook",
    )
  }
}

data class PostUpdateHookConfig(
  val commands: List<String>,
  val filesToCommit: List<String>,
  val runFor: HookRunFor,
  val commitMessage: String,
)
