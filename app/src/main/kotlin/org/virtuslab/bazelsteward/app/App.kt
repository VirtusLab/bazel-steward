package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.bazel.BuildFileSearch
import org.virtuslab.bazelsteward.bazel.FileUpdateSearch
import org.virtuslab.bazelsteward.bazel.UpdateLogic
import org.virtuslab.bazelsteward.core.Workspace
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val workspace = Workspace(Path(args[1]))
      val definitions = BuildFileSearch(workspace).buildDefinitions
      val result: String = runBlocking {
        val mavenData = MavenDataExtractor(workspace).extract()
        val availableVersions = MavenRepository().findVersions(mavenData)
        val updateSuggestions =
          availableVersions.map { UpdateLogic().selectUpdate(it.key, it.value) }.flattenOption()
        val changeSuggestions = FileUpdateSearch(definitions).searchBuildFiles(updateSuggestions)
        changeSuggestions.toString()
      }
      println(result)
    }
  }
}
