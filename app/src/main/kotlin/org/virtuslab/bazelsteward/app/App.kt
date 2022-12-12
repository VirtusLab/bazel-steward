package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.bazel.BuildFileSearch
import org.virtuslab.bazelsteward.bazel.FileUpdateSearch
import org.virtuslab.bazelsteward.bazel.UpdateLogic
import org.virtuslab.bazelsteward.core.Workspace
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val path = if (args.size > 1) args[1] else "."
      val workspace = Workspace(Path(path))
      val definitions = BuildFileSearch(workspace).buildDefinitions
      val result: String = runBlocking {
        val currentDependencies = MavenDependencyExtractor(workspace).extract()
        val availableVersions = MavenRepository().findVersions(currentDependencies)
        val updateSuggestions =
          availableVersions.map { UpdateLogic().selectUpdate(it.key, it.value) }.flattenOption()
        val changeSuggestions = FileUpdateSearch(definitions).searchBuildFiles(updateSuggestions)
        changeSuggestions.toString()
      }
      println(result)
    }
  }
}
