package org.virtuslab.bazelsteward.core.common

import java.nio.file.Path
import kotlin.io.path.readText

interface TextFile {
  val path: Path
  val content: String

  fun map(f: (String) -> String): TextFile = MappedTextFile(this, f)

  private class MappedTextFile(
    private val file: TextFile,
    private val transform: (String) -> String,
  ) : TextFile {

    override val path: Path
      get() = file.path

    override val content: String by lazy {
      transform(file.content)
    }

    override fun toString(): String = file.toString()
  }

  private class LazyTextFile(override val path: Path) : TextFile {
    override val content: String
      get() = path.readText()

    override fun toString(): String = path.toString()
  }

  companion object {
    fun from(path: Path): TextFile = LazyTextFile(path)
  }
}
