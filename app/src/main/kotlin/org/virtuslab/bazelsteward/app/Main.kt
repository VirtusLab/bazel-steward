package org.virtuslab.bazelsteward.app

import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.core.Environment

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
