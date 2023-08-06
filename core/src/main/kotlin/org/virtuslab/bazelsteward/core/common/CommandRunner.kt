package org.virtuslab.bazelsteward.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class CommandRunner {
  data class Result(val exitCode: Int, val stdout: String, val stderr: String) {
    val isSuccess: Boolean
      get() = exitCode == 0
  }

  companion object {
    suspend fun runForOutput(directory: Path, vararg command: String): String {
      return runForOutput(command.toList(), directory)
    }

    suspend fun runForOutput(command: List<String>, directory: Path? = null): String {
      val result = run(command, directory)
      if (result.isSuccess) {
        return result.stdout
      } else {
        val message = "${command.joinToString(" ")}\n${result.stdout}\n${result.stderr}"
        throw RuntimeException(message)
      }
    }

    suspend fun run(command: List<String>, directory: Path?): Result {
      logger.info { command.joinToString(" ") { if (it.contains(" ")) """"$it"""" else it } }
      return withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(command).apply {
          directory?.let { directory(it.toFile()) }
        }
        val process = processBuilder.start()
          .onExit().await()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        Result(process.exitValue(), stdout, stderr)
      }
    }
  }
}
