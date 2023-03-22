package org.virtuslab.bazelsteward.fixture

import kotlinx.coroutines.runBlocking
import org.virtuslab.bazelsteward.config.repo.RepoConfig
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.TextFile
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private data class TestTextFile(override val path: Path, override val content: String) : TextFile

internal object Resources

private fun copyFilesFromJar(resourcePath: String, targetPath: (Path) -> Path) {
  val cl = Resources.javaClass.classLoader
  val jarWithResources = URI.create(cl.getResource(resourcePath)!!.toString().split("!")[0])
  val fs = FileSystems.newFileSystem(jarWithResources, emptyMap<String, String>())
  fs.use {
    Files.walk(fs.getPath(resourcePath)).use { stream ->
      stream.filter { it.isRegularFile() }.forEach { fileInJar ->
        val target = targetPath(fileInJar).let {
          it.resolveSibling(it.name.removeSuffix(".bzlignore"))
        }
        Files.createDirectories(target.parent)
        Files.copy(fileInJar, target, StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }
}

// For `resourcePath` rules/apple will prepare workspace as `$tmpDir/rules/apple/**`
// For each extraDir, will copy its contents into target workspace,
// i.e. for extraDir `rules/base`, will copy contents of `rules/base` to `$tmpDir/rules/apple/`
fun prepareLocalWorkspace(
  tempDir: Path,
  resourcePath: String,
  extraDirs: List<String> = emptyList(),
): Path {
  val localRepo = tempDir.resolve("local")
  val workspacePath = localRepo.resolve(resourcePath)

  extraDirs.forEach { basePath ->
    copyFilesFromJar(basePath) { fileInJar ->
      workspacePath.resolve(fileInJar.toString().removePrefix(basePath.removeSuffix("/") + "/"))
    }
  }

  copyFilesFromJar(resourcePath) { fileInJar ->
    localRepo.resolve(fileInJar.toString())
  }

  return workspacePath
}

fun prepareRemoteWorkspace(tempDir: Path, testResourcePath: String, workspace: Path, master: String) {
  runBlocking {
    val remoteRepo = tempDir.resolve("remote")
    remoteRepo.createDirectories()
    GitClient(remoteRepo).init(bare = true)

    val git = GitClient(workspace)
    git.init(initialBranch = master)
    git.configureAuthor("bazel-steward@virtuslab.org", "Bazel Steward")
    git.add(Paths.get("."))
    git.commit("Maven test $testResourcePath")
    git.remoteAdd("origin", remoteRepo.toString())
    git.push(master)
  }
}

fun loadRepoConfigFromResources(resourceName: String): RepoConfig {
  return RepoConfigParser().parse(Resources.javaClass.classLoader.getResource(resourceName)!!.readText())
}

fun loadTextFileFromResources(fileName: String): TextFile {
  val url = Resources.javaClass.classLoader.getResource(fileName)!!
  return TestTextFile(Path(url.path), url.readText())
}
