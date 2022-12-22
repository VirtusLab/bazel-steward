package org.virtuslab.bazelsteward.core

import java.nio.file.Path

data class Workspace(val path: Path, val gitHostToken: String?){
  data class GitRepository(val repoName: String)
}
