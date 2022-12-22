package org.virtuslab.bazelsteward.app

import arrow.core.computations.ResultEffect.bind
import arrow.core.flattenOption
import arrow.core.identity
import kotlinx.coroutines.runBlocking
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.common.FileUpdateSearch
import org.virtuslab.bazelsteward.common.GitService
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.core.Workspace
import org.virtuslab.bazelsteward.github.createWorkspaceGithubActions
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val parser = ArgParser("bazel-steward")
      val repository by parser.argument(ArgType.String, description = "Location of repository to scan").optional()
      val github by parser.option(ArgType.Boolean, description = "Create PRs at github").default(false)
      val `github-action` by parser.option(ArgType.Boolean, description = "Running in github actions runner")
        .default(false)
      parser.parse(args)

      runBlocking {
        val workspace = createWorkspaceGithubActions().fold({ throw it }, ::identity)
        val definitions = BazelFileSearch(workspace).buildDefinitions
        val currentDependencies = MavenDependencyExtractor(workspace).extract()
        val availableVersions = MavenRepository().findVersions(currentDependencies)
        val updateSuggestions =
          availableVersions.map {
            UpdateLogic()
              .selectUpdate(it.key, it.value)
          }.flattenOption()
        val changeSuggestions = FileUpdateSearch(definitions).searchBuildFiles(updateSuggestions)
        val git = GitService(workspace)
        changeSuggestions.forEach { git.createBranchWithChange(it) }
      }
    }
  }
}
