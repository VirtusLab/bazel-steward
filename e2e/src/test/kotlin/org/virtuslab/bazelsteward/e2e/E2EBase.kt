package org.virtuslab.bazelsteward.e2e

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.eclipse.jgit.api.Git
import org.virtuslab.bazelsteward.core.GitBranch
import java.io.File
import java.util.jar.JarFile

open class E2EBase {
  protected val branchPrefix = "refs/heads/${GitBranch.branchPrefix}"
  protected val masterBranch = "refs/heads/master"
  protected fun loadTest(tempDir: File, testResourcePath: String): File {
    val jarFile = File(javaClass.protectionDomain.codeSource.location.toURI())
    val names = JarFile(jarFile).use { jar ->
      val entries = jar.entries().asIterator().asSequence()
      entries.filterNot { it.isDirectory }.map { it.name }.filter { it.startsWith(testResourcePath) }.toList()
    }
    names.forEach {
      FileUtils.copyURLToFile(
        javaClass.classLoader.getResource(it),
        File(tempDir, it.removeSuffix(".bzlignore"))
      )
    }
    val finalFile = File(tempDir, testResourcePath)
    val git = Git.init().setGitDir(File(finalFile, ".git")).call()
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Maven test $testResourcePath").setAuthor("Jan Pawel", "jp2@2137.pl").call()
    return finalFile
  }

  protected fun checkForBranches(tempDir: File, branches: List<String>) {
    val git = Git.open(tempDir)
    val gitBranches = git.branchList().call().toList().map { it.name }
    Assertions.assertThat(gitBranches).containsExactlyInAnyOrderElementsOf(branches)
  }
}
