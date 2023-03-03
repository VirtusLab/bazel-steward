package org.virtuslab.bazelsteward.common

import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.virtuslab.bazelsteward.config.repo.RepoConfig
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.common.TextFile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private data class TestTextFile(override val path: Path, override val content: String) : TextFile

fun prepareLocalWorkspace(javaClass: Class<Any>, tempDir: Path, testResourcePath: String): Path {
  val localRepo = tempDir.resolve("local")
  val finalFile = localRepo.resolve(testResourcePath)
  val names = JarFile(File(javaClass.protectionDomain.codeSource.location.toURI())).use { jar ->
    val entries = jar.entries().asIterator().asSequence()
    entries.filterNot { it.isDirectory }.map { it.name }.filter { it.startsWith(testResourcePath) }.toList()
  }
  names.forEach { name ->
    FileUtils.copyURLToFile(
      javaClass.classLoader.getResource(name),
      localRepo.resolve(name.removeSuffix(".bzlignore")).toFile()
    )
  }

  return finalFile
}

fun prepareRemoteWorkspace(tempDir: Path, testResourcePath: String, finalFile: Path, master: String) {
  runBlocking {
    val remoteRepo = tempDir.resolve("remote")
    remoteRepo.createDirectories()
    GitClient(remoteRepo).init(bare = true)

    val git = GitClient(finalFile)
    git.init(initialBranch = master)
    git.configureAuthor("bazel-steward@virtuslab.org", "Bazel Steward")
    git.add(Paths.get("."))
    git.commit("Maven test $testResourcePath")
    git.remoteAdd("origin", remoteRepo.toString())
    git.push(master)
  }
}

fun loadRepoConfigFromResources(javaClass: Class<Any>, resourceName: String): RepoConfig {
  return RepoConfigParser().parse(javaClass.classLoader.getResource(resourceName)!!.readText())
}

fun loadTextFileFromResources(javaClass: Class<Any>, fileName: String): TextFile {
  val url = javaClass.classLoader.getResource(fileName)!!
  return TestTextFile(Path(url.path), url.readText())
}
