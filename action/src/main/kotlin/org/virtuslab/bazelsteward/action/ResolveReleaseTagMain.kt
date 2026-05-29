package org.virtuslab.bazelsteward.action

import kotlin.system.exitProcess

fun main(args: Array<String>) {
  try {
    when {
      args.isNotEmpty() -> {
        val actionRef = args[0]
        val repository = args.getOrElse(1) { "VirtusLab/bazel-steward" }
        val tag = ReleaseTagResolver.resolve(actionRef, repository, ProcessGhReleaseMetadataProvider())
        println(tag)
      }
      else -> {
        System.err.println(
          "Usage: resolve-release-tag <action_ref> [repository]",
        )
        exitProcess(2)
      }
    }
  } catch (e: Exception) {
    System.err.println("ERROR: ${e.message}")
    exitProcess(1)
  }
}
