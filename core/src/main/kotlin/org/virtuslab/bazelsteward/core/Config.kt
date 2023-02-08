package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.common.GitClient.Companion.GitAuthor
import java.nio.file.Path

data class Config(
  val path: Path,
  val configPath: Path,
  val pushToRemote: Boolean,
  val baseBranch: String,
  val gitAuthor: GitAuthor
)
