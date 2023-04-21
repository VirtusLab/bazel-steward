package org.virtuslab.bazelsteward.bazel.rules

import mu.KotlinLogging
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Suppress("DEPRECATION")
class GithubRulesResolver(private val gitHubClient: GitHub) : RulesResolver {

  override fun resolveRuleVersions(ruleId: RuleLibraryId): List<RuleVersion> {
    val repositoryId = "${ruleId.repoName}/${ruleId.ruleName}"
    val repository = gitHubClient.getRepository(repositoryId)
    val releases = repository.listReleases()
    return releases.map { toVersion(ruleId, it) }.toList()
  }

  private fun toVersion(ruleId: RuleLibraryId, release: GHRelease): RuleVersion {
    val sha = sha256Regex.findAll(release.body).map { it.value }.singleOrNull()
    return RuleVersion.create(release.tagName, release.published_at.toInstant()) { resolveDetails(ruleId, release, sha) }
  }

  private class RuleDetailsCandidate(
    ruleId: RuleLibraryId,
  ) {
    val artifactName = ruleId.artifactName
    val url = ruleId.downloadUrl
    val isUrlAccessible: Boolean by lazy { testUrl(url) }
    val sha256: String by lazy { computeSha256(URL(url)) }
    fun toDetails() = RuleVersion.Details(sha256, url)
  }

  private fun resolveDetails(ruleId: RuleLibraryId, release: GHRelease, sha256: String?): RuleVersion.Details {
    val defaultCandidate = candidateByNameReplacement(ruleId, release)

    if (defaultCandidate.isUrlAccessible) {
      if (sha256 == null || sha256 == defaultCandidate.sha256) {
        return defaultCandidate.toDetails()
      }
    }

    val otherCandidates = candidatesFromAssetsAndBody(release)
      .sortedBy { levenshtein(it.artifactName, ruleId.artifactName) }

    val secondaryCandidate = otherCandidates
      .filter { it.isUrlAccessible }
      .filter { sha256 == null || sha256 == it.sha256 }
      .firstOrNull()

    if (secondaryCandidate != null) {
      return secondaryCandidate.toDetails()
    }

    if (sha256 != null) {
      val allCandidates = listOf(defaultCandidate) + otherCandidates
      val candidate = allCandidates.find { it.isUrlAccessible }
      if (candidate != null) {
        logger.warn {
          "Checksum specified in release body does not match the one computed from the artifact. " +
            "Using checksum from the body. It may be a mistake in release notes, compromised artifact " +
            "or a change in URL scheme that Bazel Steward could not replace. Use plain strings for urls of rules " +
            "to avoid this problem."
        }
        return candidate.toDetails().copy(sha256 = sha256)
      } else {
        logger.warn {
          "Could not resolve any working URL for updating ${ruleId.downloadUrl} to ${release.tagName}. " +
            "The URL may be invalid."
        }
        return defaultCandidate.toDetails()
      }
    } else {
      logger.warn {
        "Could not resolve any working URL for updating ${ruleId.downloadUrl} to ${release.tagName}. " +
          "The URL may be invalid."
      }
      return defaultCandidate.toDetails()
    }
  }

  private fun candidatesFromAssetsAndBody(release: GHRelease): Sequence<RuleDetailsCandidate> {
    val urlsFromAssets = release.assets().map { it.browserDownloadUrl }.asSequence()
    val urlsFromBody = urlRegex.findAll(release.body).map { it.groupValues.first() }
      .filterNot { urlsFromAssets.contains(it) }
    val urlCandidates = urlsFromAssets + urlsFromBody
    return urlCandidates
      .mapNotNull { runCatching { RuleDetailsCandidate(RuleLibraryId.from(it)) }.getOrNull() }
  }

  private fun candidateByNameReplacement(ruleId: RuleLibraryId, release: GHRelease): RuleDetailsCandidate {
    val newArtifactName = ruleId.artifactName.replace(ruleId.tag, release.tagName)
    return RuleDetailsCandidate(ruleId.copy(tag = release.tagName, artifactName = newArtifactName))
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
    .followRedirects(HttpClient.Redirect.NORMAL)
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
