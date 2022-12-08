package org.virtuslab.bazelsteward.maven

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.core.Workspace
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path

class MavenDependencyExtractor(private val workspace: Workspace) {
  private val bazelQuery = "bazel query @maven//:outdated.artifacts --output xml --noshow_progress"
  private val xmlRegex = Regex("""<source-file location="(.*):1:1" name="@maven//:outdated.artifacts">""")

  suspend fun extract(): List<MavenCoordinates> {
    return withContext(Dispatchers.IO) {
      val process =
        ProcessBuilder(bazelQuery.split(' ')).directory(workspace.path.toFile()).start().onExit().await()
      val xml = process.inputStream.bufferedReader().use { it.readText() }
      val fileLocation =
        xmlRegex.find(xml)?.let { it.groups[1]?.value } ?: throw RuntimeException("Failed to find file")

      Files.readAllLines(Path.of(fileLocation))
        .toList().map {
          val split = it.split(':', limit = 3)
          MavenCoordinates.of(split[0], split[1], split[2])
        }
    }
  }
}
