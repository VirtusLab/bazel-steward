package org.virtuslab.bazelsteward.bazel

import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class BazelVersionFileSearch(config: Config) {

  private val fileNames = setOf(".bazelversion", ".bazeliskrc")

  val bazelVersionFiles: List<BazelFileSearch.BazelFile> by lazy {
    config.path.listDirectoryEntries().filter { fileNames.contains(it.name) }.map { BazelFileSearch.BazelFile(it) }
  }
}
