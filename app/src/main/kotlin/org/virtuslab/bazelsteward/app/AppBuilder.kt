package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.app.provider.*
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.FileFinder
import org.virtuslab.bazelsteward.core.GitPlatform
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdateResolver
import org.virtuslab.bazelsteward.github.GithubPlatform
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
    val noRemote by parser.option(
      ArgType.Boolean,
      description = "Do not push to remote",
      fullName = "no-remote",
      shortName = "n",
    ).default(false)
    val updateAllPullRequests by parser.option(
      ArgType.Boolean,
      description = "Update all pull requests",
      fullName = "update-all-prs",
      shortName = "f",
    ).default(false)
    val baseBranch by parser.option(
      ArgType.String,
      fullName = "base-branch",
      description = "Branch that will be set as a base in pull request",
    )
    val configPath by parser.option(
      ArgType.String,
      fullName = "config-path",
      description = "Path to the config file",
    )
    val noInternalConfig by parser.option(
      ArgType.Boolean,
      fullName = "no-internal-config",
      description = "Do not load internal default config",
    ).default(false)

    parser.parse(args)

    val repositoryRoot = Path(repository).let { if (github) GithubPlatform.resolveRepoPath(env, it) else it }
    val gitClient = GitClient(repositoryRoot)
    val baseBranchName = baseBranch ?: runBlocking {
      gitClient.run("rev-parse", "--abbrev-ref", "HEAD").trim()
    }
    val gitAuthor = runBlocking { gitClient.getAuthor() }

    val appConfig = AppConfig(
      repositoryRoot,
      !noRemote,
      updateAllPullRequests,
      baseBranchName,
      gitAuthor,
    )
    logger.info { appConfig }

    val repoConfig = runBlocking { RepoConfigParser().load(configPath?.let { Path(it) }, repositoryRoot, noInternalConfig) }
    val mavenDataExtractor = MavenDataExtractor(appConfig.workspaceRoot)
    val mavenRepository = MavenRepository()
    val updateLogic = UpdateLogic()
    val gitOperations = GitOperations(appConfig.workspaceRoot, appConfig.baseBranch)
    val gitPlatform = if (github) {
      GithubPlatform.create(env, appConfig.baseBranch, appConfig.gitAuthor)
    } else {
      logger.warn {
        """Using stub client for git host. Pull Request management will not work correctly.
        |Use --github flag to enable GitHub support. Other Platforms are not supported yet.
        """.trimMargin()
      }
      GitPlatform.stub
    }
    val bazelRulesExtractor = BazelRulesExtractor()
    val bazelUpdater = BazelUpdater()
    val githubApi = (
      env["GITHUB_TOKEN"]
        ?.let(GitHub::connectUsingOAuth)
        ?: GitHub.connectAnonymously()
      )
    val githubRulesResolver = GithubRulesResolver(githubApi)
    val fileFinder = FileFinder(appConfig.workspaceRoot)

    val dependencyKinds = listOf(
      BazelVersionDependencyKind(bazelUpdater),
      BazelRulesDependencyKind(bazelRulesExtractor, githubRulesResolver),
      MavenDependencyKind(mavenDataExtractor, mavenRepository),
    )

    val libraryUpdateResolver = LibraryUpdateResolver()

    val updateRulesProvider = UpdateRulesProvider(repoConfig.updateRules, dependencyKinds)
    val searchPatternProvider = SearchPatternProvider(repoConfig.searchPaths, dependencyKinds)
    val pullRequestConfigProvider = PullRequestConfigProvider(repoConfig.pullRequests, dependencyKinds)
    val postUpdateHookProvider = PostUpdateHookProvider(repoConfig.postUpdateHooks, dependencyKinds)

    val textFileResolver = TextFileResolver(
      searchPatternProvider,
      fileFinder,
    )

    val pullRequestsPrefixesProvider = PullRequestsPrefixesProvider(repoConfig.pullRequests)

    val pullRequestsLimitsProvider = PullRequestsLimitsProvider(
      repoConfig.pullRequests,
      gitPlatform,
      appConfig.updateAllPullRequests,
      pullRequestsPrefixesProvider
    )
    val pullRequestSuggester = PullRequestSuggester(pullRequestConfigProvider)
    val pullRequestManager = PullRequestManager(
      gitPlatform,
      gitOperations,
      postUpdateHookProvider,
      appConfig.workspaceRoot,
      appConfig.pushToRemote,
      pullRequestsLimitsProvider,
    )

    return App(
      gitOperations,
      dependencyKinds,
      updateLogic,
      libraryUpdateResolver,
      pullRequestSuggester,
      repoConfig,
      updateRulesProvider,
      textFileResolver,
      pullRequestManager,
      appConfig.workspaceRoot,
    )
  }
}
