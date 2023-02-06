package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Path
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readText

class BazelFileSearch(config: Config) {
  data class BazelFile(val path: Path) {
    val content: String
      get() = path.readText()
  }

  enum class BazelFileType {
    Build, Workspace, Bzl
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
    if (paths.containsAll(workspaceFileNames)) {
      paths.remove(workspaceFileNames[0])
    }
    paths.associateWith {
      when (it.fileName.toString()) {
        in buildFileNames -> BazelFileType.Build
        "WORKSPACE", "WORKSPACE.bazel" -> BazelFileType.Workspace
        else -> BazelFileType.Bzl
      }
    }
  }

  val buildDefinitions: Map<BazelFile, BazelFileType> by lazy { buildPaths.mapKeys { BazelFile(it.key) } }
}
