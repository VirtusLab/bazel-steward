package org.virtuslab.bazelsteward.app

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.app.provider.PostUpdateHookProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestConfigProvider
import org.virtuslab.bazelsteward.app.provider.PullRequestsLimitsProvider
import org.virtuslab.bazelsteward.app.provider.SearchPatternProvider
import org.virtuslab.bazelsteward.app.provider.UpdateRulesProvider
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModDataExtractor
import org.virtuslab.bazelsteward.bzlmod.BzlModDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModRepository
import org.virtuslab.bazelsteward.config.repo.MavenConfig
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
      .optional()
    val github by parser.option(ArgType.Boolean, description = "Run as a github action").default(false)
    val noRemote by parser.option(
      ArgType.Boolean,
      description = "Do not push to remote",
      fullName = "no-remote",
      shortName = "n",
    ).default(false)
    val analyzeOnly by parser.option(
      ArgType.Boolean,
      description = "Only analyze what updates are needed",
      fullName = "analyze-only",
      shortName = "a",
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

    val repositoryRoot = repository?.let { Path(it) }
      ?: (if (github) GithubPlatform.resolveRepoPath(env, Path(".")) else Path("."))

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

    val repoConfig =
      runBlocking { RepoConfigParser().load(configPath?.let { Path(it) }, repositoryRoot, noInternalConfig) }
    val mavenConfig = repoConfig.maven ?: MavenConfig()
    val mavenDataExtractor = MavenDataExtractor(appConfig.workspaceRoot, mavenConfig.repositoryName)
    val mavenRepository = MavenRepository()
    val updateLogic = UpdateLogic()
    val gitOperations = runBlocking { GitOperations.resolve(appConfig.workspaceRoot, appConfig.baseBranch) }
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

    val bzlModDataExtractor = BzlModDataExtractor(appConfig.workspaceRoot)
    val bzlModRepository = BzlModRepository()
    val dependencyKinds = listOf(
      BazelVersionDependencyKind(bazelUpdater),
      BzlModDependencyKind(bzlModDataExtractor, bzlModRepository),
      BazelRulesDependencyKind(bazelRulesExtractor, githubRulesResolver),
      MavenDependencyKind(mavenDataExtractor, mavenRepository),
    )

    val libraryUpdateResolver = LibraryUpdateResolver()

    val updateRulesProvider = UpdateRulesProvider(repoConfig.updateRules, dependencyKinds)
    val searchPatternProvider = SearchPatternProvider(repoConfig.searchPaths, dependencyKinds)
    val pullRequestConfigProvider = PullRequestConfigProvider(repoConfig.pullRequests, dependencyKinds)
    val postUpdateHookProvider = PostUpdateHookProvider(repoConfig.postUpdateHooks, dependencyKinds)

    val textFileResolver = DefaultTextFileResolver(
      searchPatternProvider,
      fileFinder,
    )
    bzlModDataExtractor.setFileResolver(textFileResolver)

    val pullRequestsLimitsProvider = PullRequestsLimitsProvider(
      repoConfig.pullRequests,
      gitPlatform,
      appConfig.updateAllPullRequests,
      pullRequestConfigProvider,
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
      analyzeOnly,
    )
  }
}
