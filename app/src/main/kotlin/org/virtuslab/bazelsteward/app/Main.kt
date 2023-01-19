package org.virtuslab.bazelsteward.app

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.Environment

private val logger = KotlinLogging.logger {}
class Main {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      logger.info { args }
      val ctx = Context.fromArgs(args, Environment.system)
      runBlocking {
        App(ctx).run()
      }
    }
  }
}
