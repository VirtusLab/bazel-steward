package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.library.LibraryId

sealed class RuleLibraryId : LibraryId() {
  abstract val sha256: String
  abstract val tag: String
  abstract val artifactName: String
  abstract val repoName: String
  abstract val ruleName: String
  abstract val downloadUrl: String
  override val name: String
    get() = ruleName

  override fun associatedStrings(): List<String> = listOf(downloadUrl, sha256)

  data class ReleaseArtifact(
    override val sha256: String,
    override val tag: String,
    override val artifactName: String,
    override val repoName: String,
    override val ruleName: String,
  ) : RuleLibraryId() {
    override val downloadUrl: String
      get() = "https://github.com/$repoName/$ruleName/releases/download/$tag/$artifactName"

    companion object {
      private val regex = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/releases/download/(?<tag>[^/]+)/(?<artifactName>[^/]+)""")
      fun from(url: String, sha256: String): ReleaseArtifact? =
        regex.matchEntire(url)?.let { result ->
          ReleaseArtifact(
            sha256,
            result.groups["tag"]!!.value,
            result.groups["artifactName"]!!.value,
            result.groups["repoName"]!!.value,
            result.groups["ruleName"]!!.value,
          )
        }
    }
  }

  data class ArchiveTagRuleId(
    override val sha256: String,
    override val tag: String,
    override val artifactName: String,
    override val repoName: String,
    override val ruleName: String,
  ) : RuleLibraryId() {
    override val downloadUrl: String
      get() = "https://github.com/$repoName/$ruleName/archive/refs/tags/$artifactName"

    companion object {
      private val regex = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/archive/refs/tags/(?<artifactName>[^/]+)""")
      fun from(url: String, sha256: String): ArchiveTagRuleId? =
        regex.matchEntire(url)
          ?.takeIf { result -> isArtifactInAllowedFormat(result.groups["artifactName"]!!.value) }
          ?.let { result ->
            ArchiveTagRuleId(
              sha256,
              result.groups["artifactName"]!!.value.removeSuffixes(allowedArtifactExtensions),
              result.groups["artifactName"]!!.value,
              result.groups["repoName"]!!.value,
              result.groups["ruleName"]!!.value,
            )
          }
    }
  }

  data class ArchiveRuleId(
    override val sha256: String,
    override val tag: String,
    override val artifactName: String,
    override val repoName: String,
    override val ruleName: String,
  ) : RuleLibraryId() {
    override val downloadUrl: String
      get() = "https://github.com/$repoName/$ruleName/archive/$artifactName"

    companion object {
      private val regex = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/archive/(?<artifactName>[^/]+)""")
      fun from(url: String, sha256: String): ArchiveRuleId? =
        regex.matchEntire(url)
          ?.takeIf { result -> isArtifactInAllowedFormat(result.groups["artifactName"]!!.value) }
          ?.let { result ->
            ArchiveRuleId(
              sha256,
              result.groups["artifactName"]!!.value.removeSuffixes(allowedArtifactExtensions),
              result.groups["artifactName"]!!.value,
              result.groups["repoName"]!!.value,
              result.groups["ruleName"]!!.value,
            )
          }
    }
  }

  companion object {
    private val allowedArtifactExtensions = listOf(".zip", ".tar.gz", ".tgz", ".tar")
    private fun String.removeSuffixes(suffixes: Collection<String>): String = suffixes.fold(this) { str, suffix -> str.removeSuffix(suffix) }
    private fun isArtifactInAllowedFormat(artifact: String): Boolean = allowedArtifactExtensions.any { artifact.endsWith(it) }

    fun from(url: String, sha256: String): RuleLibraryId =
      ReleaseArtifact.from(url, sha256) ?: ArchiveTagRuleId.from(url, sha256) ?: ArchiveRuleId.from(url, sha256)
        ?: throw RuntimeException("Unrecognised artifact URL format $url")
  }
}
