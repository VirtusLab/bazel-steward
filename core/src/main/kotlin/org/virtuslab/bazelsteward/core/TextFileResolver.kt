package org.virtuslab.bazelsteward.core

import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.library.Library

interface TextFileResolver {
  fun resolve(library: Library): List<TextFile>
}
