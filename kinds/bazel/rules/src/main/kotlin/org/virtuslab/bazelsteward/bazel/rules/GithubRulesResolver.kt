package org.virtuslab.bazelsteward.bazel.rules

import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class GithubRulesResolver(private val gitHubClient: GitHub) : RulesResolver {

  override fun resolveRuleVersions(ruleId: RuleLibraryId): List<RuleVersion> {
    val repositoryId = "${ruleId.repoName}/${ruleId.ruleName}"
    val repository = gitHubClient.getRepository(repositoryId)
    val releases = repository.listReleases()
    return releases.map { toVersion(ruleId, it) }.toList()
  }

  private fun toVersion(ruleId: RuleLibraryId, release: GHRelease): RuleVersion {
    val sha = sha256Regex.findAll(release.body).map { it.value }.singleOrNull()
    val downloadUrl = resolveUrl(ruleId, release)
    return RuleVersion.create(downloadUrl, sha, release.tagName, release.published_at.toInstant())
  }

  private fun resolveUrl(ruleId: RuleLibraryId, release: GHRelease): String {
    val newArtifactName = ruleId.artifactName.replace(ruleId.tag, release.tagName)
    val defaultUrl = ruleId.copy(tag = release.tagName, artifactName = newArtifactName).downloadUrl

    if (testUrl(defaultUrl)) {
      return defaultUrl
    }

    val urlsFromAssets = release.assets().map { it.browserDownloadUrl }.asSequence()
    val urlsFromBody = urlRegex.findAll(release.body).map { it.groupValues.first() }
      .filterNot { urlsFromAssets.contains(it) }
      .filter { testUrl(it) }
    val urlCandidates = urlsFromAssets + urlsFromBody

    return urlCandidates
      .mapNotNull { runCatching { RuleLibraryId.from(it) }.getOrNull() }
      .sortedBy { levenshtein(it.artifactName, ruleId.artifactName) }
      .firstOrNull()?.downloadUrl
      ?: defaultUrl // hope for the best
  }

  companion object {
    private val sha256Regex = "\\b[A-Fa-f0-9]{64}\\b".toRegex()
    private val urlRegex = """(?<=")(https://github\.com/.*?)(?=")""".toRegex()

    private fun RuleLibraryId.copy(
      tag: String,
      artifactName: String,
    ): RuleLibraryId {
      return when (this) {
        is RuleLibraryId.ReleaseArtifact ->
          this.copy(tag = tag, artifactName = artifactName)

        is RuleLibraryId.ArchiveTagRuleId ->
          this.copy(tag = tag, artifactName = artifactName)

        is RuleLibraryId.ArchiveRuleId ->
          this.copy(tag = tag, artifactName = artifactName)
      }
    }
  }
}

private fun testUrl(url: String): Boolean {
  val httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()
  return runCatching {
    val uri = URI.create(url)
    val requestHead = HttpRequest.newBuilder()
      .method("HEAD", HttpRequest.BodyPublishers.noBody())
      .uri(uri)
      .build()
    val httpResponse = httpClient.send(requestHead, HttpResponse.BodyHandlers.discarding())
    return httpResponse.statusCode() < 300
  }.getOrNull() ?: false
}
