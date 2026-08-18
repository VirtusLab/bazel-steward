package org.virtuslab.bazelsteward.maven

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.common.CommandRunner
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class MavenDataExtractor(private val workspaceRoot: Path, private val bazelRepositoryName: String) {
  private val regexPattern = """<source-file location="(.*):1:1" name="@$bazelRepositoryName//:%s">"""

  suspend fun extract(): MavenData {
    val repositories = extractFromFile("outdated.repositories")
    val artifacts = parseCoordinates(extractFromFile("outdated.artifacts"))
    // outdated.boms is only written by rules_jvm_external versions that support
    // the boms attribute, hence optional.
    val boms = parseCoordinates(extractFromFile("outdated.boms", optional = true))
    return MavenData(repositories, artifacts + boms)
  }

  private suspend fun extractFromFile(fileName: String, optional: Boolean = false): List<String> = withContext(Dispatchers.IO) {
    val xml = CommandRunner.runForOutput(
      listOf(
        "bazel",
        "query",
        "@$bazelRepositoryName//:$fileName",
        "--output",
        "xml",
        "--noshow_progress",
      ),
      workspaceRoot,
      continueOnFailure = optional,
    )
    val fileLocation = Regex(regexPattern.format(fileName)).find(xml)?.groups?.get(1)?.value
      ?: if (optional) {
        logger.info { "No $fileName in @$bazelRepositoryName, skipping it" }
        return@withContext emptyList()
      } else {
        throw RuntimeException("Failed to find file: $fileName")
      }

    Path.of(fileLocation).toFile().readLines()
  }

  companion object {
    fun parseCoordinates(lines: List<String>): List<MavenCoordinates> {
      val (versioned, unversioned) = lines.filter { it.isNotBlank() }
        .map { it.split(':', limit = 3) }
        .partition { it.size == 3 && it[2].isNotBlank() }

      // An artifact declared without a version takes it from a bom, which is
      // reported separately and is the only place a bump can be applied.
      if (unversioned.isNotEmpty()) {
        logger.info {
          "Ignoring artifacts with no version of their own: " + unversioned.joinToString { it.take(2).joinToString(":") }
        }
      }

      return versioned.map { MavenCoordinates.of(it[0], it[1], it[2]) }
    }
  }
}
