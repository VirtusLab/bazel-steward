package org.virtuslab.bazelsteward.maven

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.nio.file.Path

class MavenDataExtractor(private val workspaceRoot: Path) {
  private val bazelQuery = "bazel query @maven//:%s --output xml --noshow_progress"
  private val regexPattern = """<source-file location="(.*):1:1" name="@maven//:%s">"""

  suspend fun extract(): MavenData {
    val repositories = extractFromFile("outdated.repositories")
    val dependencies = extractFromFile("outdated.artifacts").map {
      val split = it.split(':', limit = 3)
      MavenCoordinates.of(split[0], split[1], split[2])
    }
    return MavenData(repositories, dependencies)
  }

  private suspend fun extractFromFile(fileName: String): List<String> = withContext(Dispatchers.IO) {
    val process =
      ProcessBuilder(bazelQuery.format(fileName).split(' ')).directory(workspaceRoot.toFile()).start().onExit().await()
    val xml = process.inputStream.bufferedReader().use { it.readText() }
    val fileLocation = Regex(regexPattern.format(fileName)).find(xml)?.let { it.groups[1]?.value }
      ?: throw RuntimeException("Failed to find file")

    Path.of(fileLocation).toFile().readLines()
  }
}
