package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
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
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val parser = ArgParser("bazel-steward")
      val repository by parser.argument(ArgType.String, description = "Location of repository to scan").optional()
      val github by parser.option(ArgType.Boolean, description = "Create PRs at github" ).default(false)
      val `github-actions` by parser.option(ArgType.Boolean, description = "Running in github actions runner").default(false)
      parser.parse(args)

      val workspace = Workspace(Path(repository ?: "."), "token")
      val definitions = BazelFileSearch(workspace).buildDefinitions
      runBlocking {
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
