package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class CommandRunner {

  companion object {
    suspend fun run(command: List<String>, directory: Path): String {
      logger.debug { command.joinToString(" ") { if (it.contains(" ")) """"$it"""" else it } }
      return withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command).directory(directory.toFile()).start()
          .onExit().await()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        if (process.exitValue() == 0) {
          stdout
        } else {
          throw RuntimeException(
            "${command.joinToString(" ")}\n$stdout\n$stderr",
          )
        }
      }
    }
  }
}
