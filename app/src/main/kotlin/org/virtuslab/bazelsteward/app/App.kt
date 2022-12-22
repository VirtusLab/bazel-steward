package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.common.BuildFileSearch
import org.virtuslab.bazelsteward.common.FileUpdateSearch
import org.virtuslab.bazelsteward.common.GitChangeApplier
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository

class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val configuration = AppConfiguration.interpretArgs(args.toList())
      val workspace = configuration.workspace
      val definitions = BuildFileSearch(workspace).buildDefinitions
      runBlocking {
        val currentDependencies = MavenDependencyExtractor(workspace).extract()
        val availableVersions = MavenRepository().findVersions(currentDependencies)
        val updateSuggestions =
          availableVersions.map {
            UpdateLogic()
              .selectUpdate(it.key, it.value)
          }.flattenOption()
        val changeSuggestions = FileUpdateSearch(definitions).searchBuildFiles(updateSuggestions)
        val git = GitChangeApplier(workspace)
        changeSuggestions.forEach { git.applyChange(it) }
      }
    }
  }
}
