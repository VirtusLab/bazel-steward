package org.virtuslab.bazelsteward.action

data class RepositoryTag(
  val name: String,
  val commitSha: String,
)

interface GhReleaseMetadataProvider {
  fun listReleases(repository: String): List<String>

  fun listTags(repository: String): List<RepositoryTag>

  fun resolveRefToCommitSha(repository: String, ref: String): String?
}

class ProcessGhReleaseMetadataProvider : GhReleaseMetadataProvider {
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

  override fun listTags(repository: String): List<RepositoryTag> {
    val process = ProcessBuilder(
      "gh",
      "api",
      "--paginate",
      "--repo",
      repository,
      "repos/$repository/tags?per_page=100",
      "--jq",
      ".[] | [.name, .commit.sha] | @tsv",
    )
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      error("gh api tags failed for $repository (exit $exitCode): $output")
    }
    return output.lineSequence()
      .filter { it.isNotBlank() }
      .mapNotNull { line ->
        val tabIndex = line.indexOf('\t')
        if (tabIndex < 0) return@mapNotNull null
        RepositoryTag(
          name = line.substring(0, tabIndex),
          commitSha = line.substring(tabIndex + 1),
        )
      }
      .toList()
  }

  override fun resolveRefToCommitSha(repository: String, ref: String): String? {
    val process = ProcessBuilder(
      "gh",
      "api",
      "--repo",
      repository,
      "repos/$repository/commits/$ref",
      "--jq",
      ".sha",
    )
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      return null
    }
    return output.takeIf { it.matches(COMMIT_SHA_PATTERN) }
  }

  companion object {
    private val COMMIT_SHA_PATTERN = Regex("^[0-9a-fA-F]{40}$")
  }
}

object ReleaseTagResolver {
  fun resolve(
    actionRef: String,
    repository: String,
    ghMetadataProvider: GhReleaseMetadataProvider,
  ): String {
    if (actionRef.startsWith("v")) {
      return resolveLatestMatchingRelease(actionRef, ghMetadataProvider.listReleases(repository))
        ?: error("No GitHub release found matching tag pattern $actionRef in $repository")
    }
    val commitSha = if (isCommitSha(actionRef)) {
      actionRef
    } else {
      ghMetadataProvider.resolveRefToCommitSha(repository, actionRef)
        ?: error("Could not resolve ref $actionRef in $repository to a commit SHA")
    }
    return resolveReleaseForCommitSha(commitSha, repository, ghMetadataProvider)
      ?: error("No GitHub release tag in $repository points to commit $commitSha (resolved from $actionRef)")
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

  private fun resolveReleaseForCommitSha(
    commitShaRef: String,
    repository: String,
    ghMetadataProvider: GhReleaseMetadataProvider,
  ): String? {
    val releaseTags = ghMetadataProvider.listReleases(repository)
      .asSequence()
      .mapNotNull(::extractReleaseTag)
      .toSet()
    if (releaseTags.isEmpty()) return null
    return ghMetadataProvider.listTags(repository)
      .asSequence()
      .filter { releaseTags.contains(it.name) }
      .filter { matchesCommitSha(it.commitSha, commitShaRef) }
      .map { it.name }
      .maxWithOrNull(::compareVersionTags)
  }

  private fun extractReleaseTag(line: String): String? {
    val tabIndex = line.indexOf('\t')
    if (tabIndex < 0) return null
    return line.substring(0, tabIndex)
  }

  private fun matchesCommitSha(commitSha: String, commitShaRef: String): Boolean {
    val normalizedCommitSha = commitSha.lowercase()
    val normalizedCommitShaRef = commitShaRef.lowercase()
    return normalizedCommitSha == normalizedCommitShaRef || normalizedCommitSha.startsWith(normalizedCommitShaRef)
  }

  private fun isCommitSha(ref: String): Boolean = ref.matches(COMMIT_SHA_PATTERN)

  private val COMMIT_SHA_PATTERN = Regex("^[0-9a-fA-F]{7,40}$")
  private val VERSION_SUFFIX = Regex("(\\.\\d+)+")
}
