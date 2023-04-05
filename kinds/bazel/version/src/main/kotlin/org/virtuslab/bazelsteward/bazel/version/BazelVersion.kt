package org.virtuslab.bazelsteward.bazel.version

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.core.library.Version
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText

data class BazelVersion(override val value: String) : Version() {

  companion object {
    const val DOT_BAZEL_VERSION = ".bazelversion"
    const val DOT_BAZELISK_RC = ".bazeliskrc"

    suspend fun extractBazelVersion(project: Path): BazelVersion? = withContext(Dispatchers.IO) {
      val bazelVersionFile = project.resolve(DOT_BAZEL_VERSION)
      val bazeliskRcFile = project.resolve(DOT_BAZELISK_RC)
      if (bazelVersionFile.exists()) {
        bazelVersionFile.readText().takeIf { it.isNotBlank() }?.let { BazelVersion(it.trim()) }
      } else if (bazeliskRcFile.exists()) {
        bazeliskRcFile.readLines().find { it.startsWith("USE_BAZEL_VERSION") }?.substringAfter("=")?.let { BazelVersion(it.trim()) }
      } else {
        throw RuntimeException("Can't find Bazel version")
      }
    }
  }

  override val date: Date?
    get() = null
}
