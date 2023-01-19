package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.common.FileUpdateSearch
import org.virtuslab.bazelsteward.common.GitClient
import org.virtuslab.bazelsteward.common.GitOperations
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.config.BazelStewardConfiguration
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.github.GithubClient
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

data class Context(
  val config: Config,
  val bazelFileSearch: BazelFileSearch,
  val mavenDataExtractor: MavenDataExtractor,
  val mavenRepository: MavenRepository,
  val updateLogic: UpdateLogic,
  val fileUpdateSearch: FileUpdateSearch,
  val gitOperations: GitOperations,
  val gitHostClient: GitHostClient
) {

  companion object {
    fun fromArgs(args: Array<String>, env: Environment): Context {
      val parser = ArgParser("bazel-steward")
      val repository by parser.argument(ArgType.String, description = "Location of the local repository to scan")
        .optional()
      val github by parser.option(ArgType.Boolean, description = "Run as a github action").default(false)
      val pushToRemote by parser.option(ArgType.Boolean, description = "Push to remote", shortName = "p").default(false)
      parser.parse(args)
      val baseBranch by parser.argument(
        ArgType.String,
        description = "Branch that will be set as a base in pull request"
      ).optional()

      val repoPath = if (github) GithubClient.getRepoPath(env) else Path(repository ?: ".")
      val baseBranchName = baseBranch ?: runBlocking {
        GitClient(repoPath.toFile()).runGitCommand("rev-parse --abbrev-ref HEAD".split(' ')).trim()
      }

      val config = Config(repoPath, pushToRemote, baseBranchName)

      val bsc = runBlocking { BazelStewardConfiguration(repoPath).get() } // will be used later
      val bfs = BazelFileSearch(config)
      val mde = MavenDataExtractor(config)
      val mr = MavenRepository()
      val ul = UpdateLogic()
      val fus = FileUpdateSearch()
      val gc = GitOperations(config)
      val ghc = if (github) GithubClient.getClient(env, config) else GitHostClient.stub

      return Context(config, bfs, mde, mr, ul, fus, gc, ghc)
    }
  }
}
