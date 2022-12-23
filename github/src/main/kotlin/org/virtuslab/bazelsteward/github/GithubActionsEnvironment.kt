package org.virtuslab.bazelsteward.github

import arrow.core.Option
import arrow.core.getOrElse
import org.virtuslab.bazelsteward.core.Workspace
import java.lang.RuntimeException
import kotlin.io.path.Path

fun createWorkspaceGithubActions(): Workspace {
  fun getEnv(name: String) =
    Option.fromNullable(System.getenv(name))

  val url = getEnv("GITHUB_API_URL").getOrElse { throw RuntimeException() }
  val repository = getEnv("GITHUB_REPOSITORY").getOrElse { throw RuntimeException() }
  val workspace = getEnv("GITHUB_WORKSPACE").getOrElse { throw RuntimeException() }
  val token = getEnv("GITHUB_TOKEN").getOrElse { throw RuntimeException() }

  val client = GithubClient(repository, token, url)
  val path = Path(workspace)
  return Workspace(path, client)
}
