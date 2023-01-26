package org.virtuslab.bazelsteward.bazel

import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.Config
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

class BazelVersionFileSearch(config: Config) {

  private val fileNames = setOf(".bazelversion", ".bazeliskrc")

  val bazelVersionFiles: List<BazelFileSearch.BazelFile> by lazy {
    config.path.listDirectoryEntries().filter { fileNames.contains(it.name) }.map { BazelFileSearch.BazelFile(it, it.readText()) }
  }
}
