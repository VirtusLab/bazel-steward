package org.virtuslab.bazelsteward.action

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

fun interface GhReleaseLister {
  fun listReleases(repository: String): List<String>
}

class ProcessGhReleaseLister : GhReleaseLister {
  override fun listReleases(repository: String): List<String> {
    val process = ProcessBuilder("gh", "release", "list", "-L", "100", "--repo", repository)
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      error("gh release list failed for $repository (exit $exitCode): $output")
    }
    return output.lineSequence().filter { it.isNotBlank() }.toList()
  }
}

object ReleaseTagResolver {
  fun resolve(
    actionRef: String,
    actionPath: Path,
    repository: String,
    ghReleaseLister: GhReleaseLister,
  ): String {
    if (actionRef.startsWith("v")) {
      return resolveLatestMatchingRelease(actionRef, ghReleaseLister.listReleases(repository))
        ?: error("No GitHub release found matching tag pattern $actionRef in $repository")
    }
    return readReleaseTagFromActionYaml(actionPath)
  }

  fun resolveLatestMatchingRelease(pattern: String, releaseLines: List<String>): String? =
    releaseLines
      .mapNotNull { line ->
        val tabIndex = line.indexOf('\t')
        if (tabIndex < 0) return@mapNotNull null
        val tag = line.substring(0, tabIndex)
        if (matchesTagPattern(tag, pattern)) tag else null
      }
      .maxWithOrNull(::compareVersionTags)

  fun matchesTagPattern(tag: String, pattern: String): Boolean {
    if (!tag.startsWith(pattern)) return false
    val suffix = tag.removePrefix(pattern)
    return suffix.isEmpty() || suffix.matches(VERSION_SUFFIX)
  }

  fun readReleaseTagFromActionYaml(actionPath: Path): String {
    val actionYaml = actionPath.resolve("action.yaml")
    if (!actionYaml.exists()) {
      error("action.yaml not found at $actionYaml")
    }
    val tag = actionYaml.readText()
      .lineSequence()
      .map { it.trim() }
      .firstOrNull { it.startsWith("release-tag:") }
      ?.removePrefix("release-tag:")
      ?.trim()
      ?.trim('"', '\'')
      ?: error("release-tag is not set in $actionYaml (required when action ref is not a v* tag)")
    if (tag.isEmpty()) {
      error("release-tag is empty in $actionYaml")
    }
    return tag
  }

  fun compareVersionTags(left: String, right: String): Int {
    val leftParts = versionParts(left)
    val rightParts = versionParts(right)
    val max = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until max) {
      val leftValue = leftParts.getOrElse(index) { 0 }
      val rightValue = rightParts.getOrElse(index) { 0 }
      val cmp = leftValue.compareTo(rightValue)
      if (cmp != 0) return cmp
    }
    return 0
  }

  private fun versionParts(tag: String): List<Int> {
    val body = tag.removePrefix("v")
    if (body.isEmpty()) return listOf(0)
    return body.split('.').map { part ->
      part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }
  }

  private val VERSION_SUFFIX = Regex("(\\.\\d+)+")
}
