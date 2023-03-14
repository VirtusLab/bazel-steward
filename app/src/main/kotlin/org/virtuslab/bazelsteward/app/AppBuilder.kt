package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import org.virtuslab.bazelsteward.github.GithubClient
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

object AppBuilder {
  fun fromArgs(args: Array<String>, env: Environment): App {
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

    val repositoryRoot = if (github) GithubClient.getRepoPath(env) else Path(repository)
    val gitClient = GitClient(repositoryRoot)
    val baseBranchName = baseBranch ?: runBlocking {
      gitClient.run("rev-parse", "--abbrev-ref", "HEAD").trim()
    }
    val gitAuthor = runBlocking { gitClient.getAuthor() }
    val configResolvedPath = configPath?.let { Path(it) } ?: repositoryRoot.resolve(".bazel-steward.yaml")

    val appConfig = AppConfig(repositoryRoot, configResolvedPath, pushToRemote, baseBranchName, gitAuthor)
    logger.info { appConfig }

    val repoConfig = runBlocking { RepoConfigParser().load(appConfig.configPath) }
    val mavenDataExtractor = MavenDataExtractor(appConfig.workspaceRoot)
    val mavenRepository = MavenRepository()
    val updateLogic = UpdateLogic()
    val gitOperations = GitOperations(appConfig.workspaceRoot, appConfig.baseBranch)
    val gitHostClient =
      if (github) GithubClient.getClient(env, appConfig.baseBranch, appConfig.gitAuthor) else GitHostClient.stub
    val bazelRulesExtractor = BazelRulesExtractor(appConfig.workspaceRoot)
    val bazelUpdater = BazelUpdater()
    val githubRulesResolver = GithubRulesResolver(
      env["GITHUB_TOKEN"]
        ?.let(GitHub::connectUsingOAuth)
        ?: GitHub.connectAnonymously()
    )
    val fileFinder = FileFinder(appConfig.workspaceRoot)

    val dependencyKinds = listOf(
      BazelVersionDependencyKind(bazelUpdater),
      MavenDependencyKind(mavenDataExtractor, mavenRepository),
      BazelRulesDependencyKind(bazelRulesExtractor, githubRulesResolver)
    )

    val libraryUpdateResolver = LibraryUpdateResolver()

    val updateRulesProvider = UpdateRulesProvider(repoConfig.updateRules, dependencyKinds)
    val searchPatternProvider = SearchPatternProvider(repoConfig.searchPaths, dependencyKinds)
    val pullRequestProvider = PullRequestProvider(repoConfig.pullRequests, dependencyKinds)

    val libraryToTextFilesMapper = LibraryToTextFilesMapper(
      searchPatternProvider,
      fileFinder
    )

    val pullRequestSuggester = PullRequestSuggester(pullRequestProvider)

    return App(
      gitOperations,
      dependencyKinds,
      updateLogic,
      fileFinder,
      libraryUpdateResolver,
      pullRequestSuggester,
      gitHostClient,
      appConfig,
      repoConfig,
      updateRulesProvider,
      libraryToTextFilesMapper
    )
  }
}
