package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.common.GitClient.GitAuthor
import java.nio.file.Path

data class AppConfig(
  val workspaceRoot: Path,
  val configPath: Path,
  val pushToRemote: Boolean,
  val updateAllPullRequests: Boolean,
  val baseBranch: String,
  val gitAuthor: GitAuthor,
)
