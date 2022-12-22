package org.virtuslab.bazelsteward.github

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.getOrElse
import arrow.core.leftIfNull
import arrow.core.right
import org.virtuslab.bazelsteward.core.Workspace
import kotlin.io.path.Path

suspend fun createWorkspaceGithubActions(): Either<Throwable, Workspace> {
  fun getEnv(name: String) =
    System.getenv(name).right().leftIfNull { RuntimeException("$name not found in env vars") }

  return either {
    val url = getEnv("GITHUB_API_URL").getOrElse { "https://api.github.com" }
    val repository = getEnv("GITHUB_REPOSITORY").bind()
    val workspace = getEnv("GITHUB_WORKSPACE").getOrElse { "." }
    val token = getEnv("GITHUB_TOKEN").bind()

    //base branch

    val client = Either.catch { GithubClient(repository, token, url) }.bind()
    val path = Path(workspace)
    Workspace(path, client)
  }
}