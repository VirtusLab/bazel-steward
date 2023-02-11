package org.virtuslab.bazelsteward.core.common

import org.virtuslab.bazelsteward.core.Config
import java.nio.file.Path
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readText

class BazelFileSearch(config: Config) {
  interface BazelFile {
    val path: Path
    val content: String
  }

  private data class LazyBazelFile(override val path: Path) : BazelFile {
    override val content: String
      get() = path.readText()
  }

  enum class BazelFileType { Build, Workspace, Bzl }

  private val workspaceFilePaths = workspaceFileNames.map { config.path.resolve(it) }

  private val buildPaths: Map<Path, BazelFileType> by lazy {
    val paths = config.path.toFile().walkBottomUp()
      .onEnter { !(it.name.startsWith(".") || it.toPath().isSymbolicLink()) }
      .filter { it.isFile }
      .filter { buildFileNames.contains(it.name) || it.name.endsWith(fileSuffix) || workspaceFilePaths.contains(it.toPath()) }
      .map { it.toPath() }
      .toMutableList()
    if (paths.containsAll(workspaceFilePaths)) {
      paths.remove(workspaceFilePaths[0])
    }
    paths.associateWith {
      when (it.fileName.toString()) {
        in buildFileNames -> BazelFileType.Build
        in workspaceFileNames -> BazelFileType.Workspace
        else -> BazelFileType.Bzl
      }
    }
  }

  val buildDefinitions: Map<BazelFile, BazelFileType> by lazy { buildPaths.mapKeys { createBazelFile(it.key) } }

  companion object {
    private val buildFileNames = listOf("BUILD.bazel", "BUILD")
    private val workspaceFileNames = listOf("WORKSPACE", "WORKSPACE.bazel")
    private const val fileSuffix = ".bzl"
    fun createBazelFile(path: Path): BazelFile = LazyBazelFile(path)
  }
}
