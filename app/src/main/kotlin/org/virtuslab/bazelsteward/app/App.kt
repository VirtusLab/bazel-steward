package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.common.FileUpdateSearch
import org.virtuslab.bazelsteward.common.GitClient
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.core.GitHostClient
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
      val pushToRemote by parser.option(ArgType.Boolean, description = "Push to remote", shortName = "p").default(false)

      parser.parse(args)

      val workspace =
        if (github) createWorkspaceGithubActions()
        else Workspace(Path(repository ?: "."), GitHostClient.stub, pushToRemote)

      runBlocking {
        val definitions = BazelFileSearch(workspace).buildDefinitions
        val currentDependencies = MavenDependencyExtractor(workspace).extract()
        val availableVersions = MavenRepository().findVersions(currentDependencies)
        val updateSuggestions =
          availableVersions.map {
            UpdateLogic()
              .selectUpdate(it.key, it.value)
          }.flattenOption()
        val changeSuggestions = FileUpdateSearch(definitions).searchBuildFiles(updateSuggestions)
        val git = GitClient(workspace)
        val gitHost = workspace.gitHostClient
        changeSuggestions.forEach { change ->
          val branch = GitClient.Companion.fileChangeSuggestionToBranch(change)
          if (!gitHost.checkIfPrExists(branch)) {
            git.createBranchWithChange(change)
            if (workspace.pushToRemote) {
              git.pushBranchToOrigin(branch)
              gitHost.openNewPR(branch)
            }
          }
        }
      }
    }
  }
}
