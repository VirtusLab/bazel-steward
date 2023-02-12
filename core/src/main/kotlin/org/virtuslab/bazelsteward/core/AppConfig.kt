package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.common.GitClient.GitAuthor
import java.nio.file.Path

data class AppConfig(
  val workspaceRoot: Path,
  val configPath: Path,
  val pushToRemote: Boolean,
  val baseBranch: String,
  val gitAuthor: GitAuthor
)
