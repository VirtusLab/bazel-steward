package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.BazelUpdater
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileUpdateSearch
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.BazelStewardConfig
import org.virtuslab.bazelsteward.core.config.BazelStewardConfigExtractor
import org.virtuslab.bazelsteward.github.GithubClient
import org.virtuslab.bazelsteward.github.GithubRulesResolver
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import org.virtuslab.bazelsteward.rules.BazelRulesExtractor
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

data class Context(
  val config: Config,
  val bazelStewardConfig: BazelStewardConfig,
  val bazelFileSearch: BazelFileSearch,
  val mavenDataExtractor: MavenDataExtractor,
  val bazelRulesExtractor: BazelRulesExtractor,
  val mavenRepository: MavenRepository,
  val updateLogic: UpdateLogic,
  val fileUpdateSearch: FileUpdateSearch,
  val gitOperations: GitOperations,
  val gitHostClient: GitHostClient,
  val bazelUpdater: BazelUpdater,
  val githubRulesResolver: GithubRulesResolver,
) {

  companion object {
    fun fromArgs(args: Array<String>, env: Environment): Context {
      val parser = ArgParser("bazel-steward")
      val repository by parser.argument(ArgType.String, description = "Location of the local repository to scan")
        .optional().default(".")
      val github by parser.option(ArgType.Boolean, description = "Run as a github action").default(false)
      val pushToRemote by parser.option(
        ArgType.Boolean,
        description = "Push to remote",
        fullName = "push-to-remote",
        shortName = "p"
      ).default(false)
      val baseBranch by parser.option(
        ArgType.String,
        fullName = "base-branch",
        description = "Branch that will be set as a base in pull request"
      )
      val configPath by parser.option(
        ArgType.String,
        fullName = "config-path",
        description = "Path to the config file"
      )

      parser.parse(args)

      val repoPath = if (github) GithubClient.getRepoPath(env) else Path(repository)
      val gitClient = GitClient(repoPath.toFile())
      val baseBranchName = baseBranch ?: runBlocking {
        gitClient.runGitCommand("rev-parse --abbrev-ref HEAD".split(' ')).trim()
      }
      val gitAuthor = runBlocking { gitClient.getAuthor() }
      val configResolvedPath = configPath?.let { Path(it) } ?: repoPath.resolve(".bazel-steward.yaml")

      val config = Config(repoPath, configResolvedPath, pushToRemote, baseBranchName, gitAuthor)
      logger.info { config }

      val bsc = runBlocking { BazelStewardConfigExtractor(config.configPath).get() }
      val bfs = BazelFileSearch(config)
      val mde = MavenDataExtractor(config)
      val mr = MavenRepository()
      val ul = UpdateLogic()
      val fus = FileUpdateSearch()
      val gc = GitOperations(config)
      val ghc = if (github) GithubClient.getClient(env, config) else GitHostClient.stub
      val bre = BazelRulesExtractor(config)
      val bu = BazelUpdater()
      val grr = if (github) {
        GithubRulesResolver(GitHub.connectUsingOAuth(env.getOrThrow("GITHUB_TOKEN"))) // TODO: refactor
      } else {
        GithubRulesResolver(GitHub.connectAnonymously()) // TODO: this may hit rate limit pretty soon
      }

      return Context(config, bsc, bfs, mde, bre, mr, ul, fus, gc, ghc, bu, grr)
    }
  }
}
