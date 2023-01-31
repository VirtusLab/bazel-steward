package org.virtuslab.bazelsteward.rules

import org.virtuslab.bazelsteward.core.library.LibraryId

data class BazelRuleLibraryId(val url: String, val sha256: String) : LibraryId {
  override fun associatedStrings(): List<String> = listOf(url, sha256)

  val repoName: String
  val ruleName: String
  val tag: String
  val artifactName: String
  override val name: String
    get() = ruleName

  init {
    val regex1 = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/releases/download/(?<tag>[^/]+)/(?<artifactName>[^/]+)""")
    val regex2 = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/archive/(?<artifactName>[^/]+)""") // I am not sure if extracting tag from artifactName in this case will be always successful
    val regex3 = Regex("""^https://github.com/(?<repoName>[^/]+)/(?<ruleName>[^/]+)/archive/refs/tags/(?<artifactName>[^/]+)""")
    val matchResult1 = regex1.matchEntire(url)
    val matchResult2 = regex2.matchEntire(url) ?: regex3.matchEntire(url)
    if (matchResult1 != null) {
      val values = matchResult1.groups as MatchNamedGroupCollection
      repoName = values["repoName"]?.value!!
      ruleName = values["ruleName"]?.value!!
      tag = values["tag"]?.value!!
      artifactName = values["artifactName"]?.value!!
    } else if (matchResult2 != null) {
      val values = matchResult2.groups as MatchNamedGroupCollection
      repoName = values["repoName"]?.value!!
      ruleName = values["ruleName"]?.value!!
      artifactName = values["artifactName"]?.value!!
      if (!isArtifactInAllowedFormat(artifactName)) throw RuntimeException("Artifact $artifactName has an unrecognised format")
      tag = artifactName.removeSuffix(".zip").removeSuffix(".tar.gz").removeSuffix(".tgz").removeSuffix(".tar")
    } else {
      throw RuntimeException("Could not parse repository URL $url")
    }
  }
  private fun isArtifactInAllowedFormat(artifact: String): Boolean =
    listOf(".zip", ".tar.gz", ".tgz", ".tar").any { artifact.endsWith(it) }
}
