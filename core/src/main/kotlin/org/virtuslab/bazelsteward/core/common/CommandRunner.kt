package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}
class CommandRunner {

  companion object {
    suspend fun run(command: List<String>, directory: File): String {
      logger.debug { command }
      return withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command).directory(directory).start()
          .onExit().await()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        if (process.exitValue() == 0) stdout else throw RuntimeException(
          "${command.joinToString(" ")}\n$stdout\n$stderr"
        )
      }
    }
  }
}
