package org.virtuslab.bazelsteward.action

import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  try {
    when {
      args.size >= 2 && args[0] == "--read-action-yaml" -> {
        val actionPath = Paths.get(args[1])
        println(ReleaseTagResolver.readReleaseTagFromActionYaml(actionPath))
      }
      args.size >= 2 -> {
        val actionRef = args[0]
        val actionPath = Paths.get(args[1])
        val repository = args.getOrElse(2) { "VirtusLab/bazel-steward" }
        val tag = ReleaseTagResolver.resolve(actionRef, actionPath, repository, ProcessGhReleaseLister())
        println(tag)
      }
      else -> {
        System.err.println(
          "Usage: resolve-release-tag --read-action-yaml <action_path>\n" +
            "       resolve-release-tag <action_ref> <action_path> [repository]",
        )
        exitProcess(2)
      }
    }
  } catch (e: Exception) {
    System.err.println("ERROR: ${e.message}")
    exitProcess(1)
  }
}
