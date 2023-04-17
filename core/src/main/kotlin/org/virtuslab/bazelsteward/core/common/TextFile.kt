package org.virtuslab.bazelsteward.core.common

import java.nio.file.Path
import kotlin.io.path.readText

interface TextFile {
  val path: Path
  val content: String

  private class LazyTextFile(override val path: Path) : TextFile {
    override val content: String
      get() = path.readText()

    override fun toString(): String {
      return path.toString()
    }
  }

  companion object {
    fun from(path: Path): TextFile = LazyTextFile(path)
  }
}
