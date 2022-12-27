package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.common.FileUpdateSearch
import org.virtuslab.bazelsteward.common.GitClient
import org.virtuslab.bazelsteward.common.UpdateLogic
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.maven.MavenDependencyExtractor
import org.virtuslab.bazelsteward.maven.MavenRepository

class Main {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val ctx = Context.fromArgs(args, Environment.system)
      runBlocking {
        App(ctx).run()
      }
    }
  }
}
