package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Files
import java.nio.file.Path

class BazelFileSearch(config: Config) {
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

  val buildDefinitions: List<Pair<Path, String>> by lazy {
    buildPaths.map { it to String(Files.readAllBytes(it)) }
  }
}
