package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.virtuslab.bazelsteward.common.GitClient
import org.virtuslab.bazelsteward.core.GitBranch
import java.io.File
import java.lang.RuntimeException
import java.util.jar.JarFile

open class E2EBase {
  private val heads = "refs/heads/"
  protected val branchRef = "$heads${GitBranch.branchPrefix}"
  private val master = "master"
  protected val masterRef = "$heads$master"
  protected fun loadTest(tempDir: File, testResourcePath: String): File {
    val localRepo = File(tempDir, "local")
    val finalFile = File(localRepo, testResourcePath)
    val jarFile = File(javaClass.protectionDomain.codeSource.location.toURI())
    val names = JarFile(jarFile).use { jar ->
      val entries = jar.entries().asIterator().asSequence()
      entries.filterNot { it.isDirectory }.map { it.name }.filter { it.startsWith(testResourcePath) }.toList()
    }
    names.forEach {
      FileUtils.copyURLToFile(
        javaClass.classLoader.getResource(it),
        File(localRepo, it.removeSuffix(".bzlignore"))
      )
    }

    runBlocking {
      val remoteRepo = File(tempDir, "remote")
      if (!remoteRepo.mkdir())
        throw RuntimeException("Failed to create directory")
      GitClient(remoteRepo).init(bare = true)

      val git = GitClient(finalFile)
      git.init(initialBranch = master)
      git.add(File(".").toPath())
      git.commit("Maven test $testResourcePath")
      git.remoteAdd("origin", remoteRepo.path)
      git.push(master)
    }
    return finalFile
  }

  protected fun checkBranchesWithVersions(tempDir: File, testResourcePath: String, branches: List<String>) {
    val localRepo = File(File(tempDir, "local"), testResourcePath)
    val remoteRepo = File(tempDir, "remote")

    checkForBranchesWithVersions(localRepo, branches)
    checkForBranchesWithVersions(remoteRepo, branches)

    checkStatusOfBranches(localRepo, branches)
  }

  protected fun checkBranchesWithoutVersions(tempDir: File, testResourcePath: String, branchesPattern: List<String>) {
    val localRepo = File(File(tempDir, "local"), testResourcePath)
    val remoteRepo = File(tempDir, "remote")

    checkForBranchesWithoutVersions(localRepo, branchesPattern)
    checkForBranchesWithoutVersions(remoteRepo, branchesPattern)

    val git = GitClient(tempDir)
    val gitBranches = runBlocking { git.showRef(heads = true) }
    checkStatusOfBranches(localRepo, gitBranches)
  }

  private fun checkForBranchesWithoutVersions(tempDir: File, requiredBranches: List<String>) {
    val git = GitClient(tempDir)
    val gitBranches = runBlocking { git.showRef(heads = true) }

    Assertions.assertThat(gitBranches).hasSameSizeAs(requiredBranches)
    Assertions.assertThat(requiredBranches.all { branch -> gitBranches.any { it.contains(branch) } }).isTrue
  }

  private fun checkForBranchesWithVersions(tempDir: File, branches: List<String>) {
    val git = GitClient(tempDir)
    val gitBranches = runBlocking { git.showRef(heads = true) }
    Assertions.assertThat(gitBranches).containsExactlyInAnyOrderElementsOf(branches)
  }

  private fun checkStatusOfBranches(tempDir: File, branches: List<String>) {
    val git = GitClient(tempDir)
    runBlocking {
      branches.forEach { branchRef ->
        val branch = branchRef.removePrefix(heads)
        git.checkout(branch)
        val status = git.status()
        Assertions.assertThat(status)
          .contains("Your branch is up to date with")
          .contains("nothing to commit, working tree clean")
      }
    }
  }
}
