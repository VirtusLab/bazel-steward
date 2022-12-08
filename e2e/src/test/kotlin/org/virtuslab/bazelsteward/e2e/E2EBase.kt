package org.virtuslab.bazelsteward.e2e

import org.apache.commons.io.FileUtils
import java.io.File
import java.util.jar.JarFile

open class E2EBase {
  protected fun loadTest(tempDir: File, testResourcePath: String) {
    val jarFile = File(javaClass.protectionDomain.codeSource.location.toURI())
    val names = JarFile(jarFile).use { jar ->
      val entries = jar.entries().asIterator().asSequence()
      entries.filterNot { it.isDirectory }.map { it.name }.filter { it.startsWith(testResourcePath) }.toList()
    }
    names.forEach { FileUtils.copyURLToFile(javaClass.classLoader.getResource(it), File(tempDir, stripSuffix(it))) }
  }

  private fun stripSuffix(name: String): String =
    if (name.endsWith(".blzx"))
      if (name.startsWith("BUILD") || name.startsWith("WORKSPACE"))
        name.dropLast(5)
      else
        name.dropLast(1)
    else name
}
