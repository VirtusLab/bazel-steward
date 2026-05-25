package org.virtuslab.bazelsteward.app

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.Environment
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class Main {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      logger.info { args.toList() }
      val exitCode = runBlocking {
        AppBuilder.fromArgs(args, Environment.system).run().exitCode()
      }
      if (exitCode != 0) {
        exitProcess(exitCode)
      }
    }
  }
}
