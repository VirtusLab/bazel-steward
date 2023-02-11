package org.virtuslab.bazelsteward.bazel

import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class BazelVersionFileSearch(appConfig: AppConfig) {

  private val fileNames = setOf(".bazelversion", ".bazeliskrc")

  val bazelVersionFiles: List<BazelFileSearch.BazelFile> by lazy {
    appConfig.path.listDirectoryEntries().filter { fileNames.contains(it.name) }.map { BazelFileSearch.createBazelFile(it) }
  }
}
