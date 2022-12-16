package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.Workspace
import kotlin.io.path.Path

data class AppConfiguration(val workspace: Workspace, val github: Boolean) {
  companion object {
    fun interpretArgs(args: List<String>): AppConfiguration {
      val ls = args.toMutableList()
      ls.removeAt(0)
      val github = ls.contains("--github")
      ls.remove("--github")
      val path = Path(ls.firstOrNull() ?: ".")
      return AppConfiguration(Workspace(path), github = github)
    }
  }
}

