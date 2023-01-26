package org.virtuslab.bazelsteward.bazel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.core.library.Version
import java.lang.RuntimeException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText

data class BazelVersion(override val value: String) : Version {

  companion object {
    private const val bazelVersionFileName = ".bazelversion"
    private const val bazeliskRcFileName = ".bazeliskrc"

    suspend fun extractBazelVersion(project: Path): BazelVersion? = withContext(Dispatchers.IO) {
      val bazelVersionFile = project.resolve(bazelVersionFileName)
      val bazeliskRcFile = project.resolve(bazeliskRcFileName)
      if (bazelVersionFile.exists()) {
        bazelVersionFile.readText().takeIf { it.isNotBlank() }?.let { BazelVersion(it.trim()) }
      } else if (bazeliskRcFile.exists()) {
        bazeliskRcFile.readLines().find { it.startsWith("USE_BAZEL_VERSION") }?.substringAfter("=")?.let { BazelVersion(it.trim()) }
      } else {
        throw RuntimeException("Can't find Bazel version")
      }
    }
  }
}
