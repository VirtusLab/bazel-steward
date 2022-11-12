package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.UpdateLogic
import org.virtuslab.bazelsteward.core.Workspace
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyUpdater
import org.virtuslab.bazelsteward.maven.MavenRepository
import kotlin.io.path.Path

class App {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val workspace = Workspace(Path("~/dev/bazel-steward"))
            val currentDependencies = MavenDependencyExtractor().extract(workspace)
            val availableVersions = MavenRepository().findVersions(currentDependencies.map { it.id })
            val toUpdate = UpdateLogic().selectUpdates(currentDependencies, availableVersions)
            MavenDependencyUpdater().applyUpdates(toUpdate)
            println("Done")
        }
    }
}