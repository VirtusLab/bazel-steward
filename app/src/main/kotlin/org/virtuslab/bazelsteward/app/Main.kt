package org.virtuslab.bazelsteward.app

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.replacement.FileChangeSuggester

private val logger = KotlinLogging.logger {}

class Main {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      logger.info { args.toList() }
      mainMapContext(args)
    }

    fun mainMapContext(args: Array<String>, f: (Context) -> Context = { x -> x }) {
      val ctx = f(Context.fromArgs(args, Environment.system))
      val dependencyKinds = listOf(
        BazelVersionDependencyKind(ctx.bazelUpdater),
        MavenDependencyKind(ctx.mavenDataExtractor, ctx.mavenRepository),
        BazelRulesDependencyKind(ctx.bazelRulesExtractor, ctx.githubRulesResolver)
      )
      runBlocking {
        App(
          ctx.gitOperations,
          dependencyKinds,
          ctx.updateLogic,
          FileFinder(ctx.appConfig.workspaceRoot),
          FileChangeSuggester(),
          PullRequestSuggester(),
          ctx.gitHostClient,
          ctx.appConfig,
          ctx.repoConfig
        ).run()
      }
    }
  }
}
