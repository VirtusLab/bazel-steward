package org.virtuslab.bazelsteward.maven

import kotlinx.coroutines.future.await
import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Files
import java.nio.file.Path

class MavenDependencyExtractor(private val workspace: Config) {
  private val bazelQuery = "bazel query @maven//:outdated.artifacts --output xml --noshow_progress"
  private val xmlRegex = Regex("""<source-file location="(.*):1:1" name="@maven//:outdated.artifacts">""")

  @Suppress("BlockingMethodInNonBlockingContext")
  suspend fun extract(): List<MavenCoordinates> {
    val process =
      ProcessBuilder(bazelQuery.split(' ')).directory(workspace.path.toFile()).start().onExit().await()
    val xml = process.inputStream.bufferedReader().use { it.readText() }
    val fileLocation =
      xmlRegex.find(xml)?.let { it.groups[1]?.value } ?: throw RuntimeException("Failed to find file in [$xml]")

    return Files.readAllLines(Path.of(fileLocation))
      .toList().map {
        val split = it.split(':', limit = 3)
        MavenCoordinates.of(split[0], split[1], split[2])
      }
  }
}
