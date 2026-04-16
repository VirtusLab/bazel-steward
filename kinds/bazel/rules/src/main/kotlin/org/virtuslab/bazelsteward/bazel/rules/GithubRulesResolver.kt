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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

@Suppress("DEPRECATION")
class GithubRulesResolver(
  private val gitHubClient: GitHub,
  private val perRepoTimeoutSeconds: Long = DEFAULT_PER_REPO_TIMEOUT_SECONDS,
  private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) : RulesResolver {

  override fun resolveRuleVersions(ruleId: RuleLibraryId): List<RuleVersion> {
    val repositoryId = "${ruleId.repoName}/${ruleId.ruleName}"
    return retryWithBackoff(repositoryId) {
      fetchReleasesWithTimeout(ruleId, repositoryId)
    }
  }

  private fun fetchReleasesWithTimeout(ruleId: RuleLibraryId, repositoryId: String): List<RuleVersion> {
    try {
      val future = CompletableFuture.supplyAsync {
        val repository = gitHubClient.getRepository(repositoryId)
        repository.listReleases().map { toVersion(ruleId, it) }.toList()
      }
      return future.get(perRepoTimeoutSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
      logger.warn { "Timed out fetching releases for $repositoryId after ${perRepoTimeoutSeconds}s" }
      throw e
    }
  }

  private fun retryWithBackoff(repositoryId: String, action: () -> List<RuleVersion>): List<RuleVersion> {
    var lastException: Exception? = null
    for (attempt in 0..maxRetries) {
      if (attempt > 0) {
        val backoffMs = INITIAL_BACKOFF_MS * (1L shl (attempt - 1))
        logger.info { "Retrying $repositoryId (attempt ${attempt + 1}/${maxRetries + 1}) after ${backoffMs}ms" }
        Thread.sleep(backoffMs)
      }
      try {
        return action()
      } catch (e: Exception) {
        lastException = e
        val isRateLimit = isRateLimitError(e)
        if (isRateLimit) {
          logger.warn { "GitHub API rate limit hit for $repositoryId. ${rateLimitInfo()}" }
          break
        }
        if (attempt == maxRetries) break
        logger.warn { "Failed to fetch releases for $repositoryId: ${e.message}" }
      }
    }
    logger.error { "Giving up on fetching releases for $repositoryId after ${maxRetries + 1} attempts: ${lastException?.message}" }
    return emptyList()
  }

  private fun isRateLimitError(e: Exception): Boolean {
    val message = (e.cause ?: e).message ?: return false
    return message.contains("rate limit", ignoreCase = true) ||
      message.contains("API rate limit exceeded", ignoreCase = true) ||
      message.contains("403") ||
      message.contains("429")
  }

  private fun rateLimitInfo(): String {
    return try {
      val rateLimit = gitHubClient.rateLimit
      "Remaining: ${rateLimit.remaining}/${rateLimit.limit}, resets at ${rateLimit.resetDate}"
    } catch (_: Exception) {
      "Could not fetch rate limit info"
    }
  }

  private fun toVersion(ruleId: RuleLibraryId, release: GHRelease): RuleVersion {
    return RuleVersion.create(release.tagName, release.published_at.toInstant()) { resolveDetails(ruleId, release) }
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

  private fun resolveDetails(ruleId: RuleLibraryId, release: GHRelease): RuleVersion.Details {
    val sha256 = release.body?.let { body -> sha256Regex.findAll(body).map { it.value }.singleOrNull() }

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
    private const val DEFAULT_PER_REPO_TIMEOUT_SECONDS = 60L
    private const val DEFAULT_MAX_RETRIES = 2
    private const val INITIAL_BACKOFF_MS = 2000L

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
      .timeout(Duration.ofSeconds(30))
      .build()
    val httpResponse = httpClient.send(requestHead, HttpResponse.BodyHandlers.discarding())
    return httpResponse.statusCode() < 300
  }.getOrNull() ?: false
}
