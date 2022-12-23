package org.virtuslab.bazelsteward.e2e

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import java.io.File
import java.util.jar.JarFile

open class E2EBase {
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
}
