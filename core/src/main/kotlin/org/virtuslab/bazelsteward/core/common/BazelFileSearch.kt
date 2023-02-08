package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Path
import kotlin.io.path.readText

class BazelFileSearch(config: Config) {
  interface BazelFile {
    val path: Path
    val content: String
  }

  data class BazelFileLazy(override val path: Path): BazelFile {
    override val content: String
      get() = path.readText()
  }

  data class BazelFileTest(override val path: Path, override val content: String): BazelFile {
  }

  private val fileNames = setOf("""BUILD.bazel""", "BUILD", "WORKSPACE")
  private val fileSuffix = ".bzl"

  private val buildPaths: List<Path> by lazy {
    config.path.toFile().walkBottomUp()
      .onEnter { !it.name.startsWith(".") }
      .filter { it.isFile }
      .filter { fileNames.contains(it.name) || it.name.endsWith(fileSuffix) }
      .map { it.toPath() }
      .toList()
  }

  val buildDefinitions: List<BazelFile> by lazy { buildPaths.map { BazelFileLazy(it) } }
}


