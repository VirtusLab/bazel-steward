package org.virtuslab.bazelsteward.common

import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Path
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readText

class BazelFileSearch(config: Config) {
  data class BazelFile(val path: Path) {
    val content: String by lazy {
      path.readText()
    }
  }

  enum class BazelFileType {
    BUILD, WORKSPACE, BZL
  }

  private val buildFileNames = listOf("BUILD.bazel", "BUILD")
  private val workspaceFileNames = listOf("WORKSPACE", "WORKSPACE.bazel").map { config.path.resolve(it) }
  private val fileSuffix = ".bzl"

  private val buildPaths: Map<Path, BazelFileType> by lazy {
    val paths = config.path.toFile().walkBottomUp()
      .onEnter { !(it.name.startsWith(".") || it.toPath().isSymbolicLink()) }
      .filter { it.isFile }
      .filter { buildFileNames.contains(it.name) || it.name.endsWith(fileSuffix) || workspaceFileNames.contains(it.toPath()) }
      .map { it.toPath() }
      .toMutableList()
    if (paths.toList().containsAll(workspaceFileNames)) {
      paths.remove(workspaceFileNames[0])
    }
    paths.associateWith {
      when (it.fileName.toString()) {
        "BUILD.bazel", "BUILD" -> BazelFileType.BUILD
        "WORKSPACE", "WORKSPACE.bazel" -> BazelFileType.WORKSPACE
        else -> BazelFileType.BZL
      }
    }
  }

  val buildDefinitions: Map<BazelFile, BazelFileType> by lazy { buildPaths.mapKeys { BazelFile(it.key) } }
}
