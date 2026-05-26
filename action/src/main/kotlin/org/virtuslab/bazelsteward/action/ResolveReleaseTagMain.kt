package org.virtuslab.bazelsteward.action

import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  if (args.size < 2) {
    System.err.println("Usage: resolve-release-tag <action_ref> <action_path> [repository]")
    exitProcess(2)
  }
  val actionRef = args[0]
  val actionPath = Paths.get(args[1])
  val repository = args.getOrElse(2) { "VirtusLab/bazel-steward" }
  try {
    val tag = ReleaseTagResolver.resolve(actionRef, actionPath, repository, ProcessGhReleaseLister())
    println(tag)
  } catch (e: Exception) {
    System.err.println("ERROR: ${e.message}")
    exitProcess(1)
  }
}
