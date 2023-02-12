package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.BazelUpdater
import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.config.RepoConfig
import org.virtuslab.bazelsteward.core.config.RepoConfigParser
import org.virtuslab.bazelsteward.github.GithubClient
import org.virtuslab.bazelsteward.github.GithubRulesResolver
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import org.virtuslab.bazelsteward.rules.BazelRulesExtractor
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

data class Context(
  val appConfig: AppConfig,
  val repoConfig: RepoConfig,
  val mavenDataExtractor: MavenDataExtractor,
  val bazelRulesExtractor: BazelRulesExtractor,
  val mavenRepository: MavenRepository,
  val updateLogic: UpdateLogic,
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
      ).default(true)
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

      val appConfig = AppConfig(repoPath, configResolvedPath, pushToRemote, baseBranchName, gitAuthor)
      logger.info { appConfig }

      val bsc = runBlocking { RepoConfigParser(appConfig.configPath).get() }
      val mde = MavenDataExtractor(appConfig)
      val mr = MavenRepository()
      val ul = UpdateLogic()
      val gc = GitOperations(appConfig)
      val ghc = if (github) GithubClient.getClient(env, appConfig) else GitHostClient.stub
      val bre = BazelRulesExtractor(appConfig)
      val bu = BazelUpdater()
      val grr = GithubRulesResolver(env["GITHUB_TOKEN"]?.let(GitHub::connectUsingOAuth) ?: GitHub.connectAnonymously())

      return Context(appConfig, bsc, mde, bre, mr, ul, gc, ghc, bu, grr)
    }
  }
}
