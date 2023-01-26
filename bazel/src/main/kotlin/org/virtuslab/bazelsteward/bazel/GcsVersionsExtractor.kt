package org.virtuslab.bazelsteward.bazel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class GcsVersionsExtractor {

  suspend fun getVersionPrefixes(): List<BazelVersion> {
    val prefixes = listDirectoriesInReleaseBucket("")
    return getVersionsFromGCSPrefixes(prefixes)
  }

  suspend fun getAllVersions(prefix: BazelVersion): List<BazelVersion> {
    val (rollingPaths, others) = listDirectoriesInReleaseBucket("${prefix.value}/").partition { it.endsWith("rolling/") } // We have to do some more preprocessing for prereleases
    val prereleaseVersions = rollingPaths
      .flatMap { listDirectoriesInReleaseBucket(it) }
      .mapNotNull { versionPath -> versionPath.replace("/", "").substringAfter("rolling").takeUnless { it.matches(".*rc.*".toRegex()) } } // Bazelisk doesn't recognize versions like 7.0.0-pre.20230104.2rc1, which are present in the buckets
    val otherVersions = others.map { it.replace("/", "").removeSuffix("release") }
    return (prereleaseVersions + otherVersions).map(::BazelVersion)
  }

  suspend fun listDirectoriesInReleaseBucket(prefix: String): List<String> {
    val baseURL = "https://www.googleapis.com/storage/v1/b/bazel/o?delimiter=/" + ifNotBlank(prefix) { "&prefix=$prefix" }

    suspend fun queryGcs(nextPageToken: String): List<String> {
      val url = baseURL + ifNotBlank(nextPageToken) { "&pageToken=$nextPageToken" }

      val content = withContext(Dispatchers.IO) { URL(url).openStream() }.use { it.bufferedReader().readText() }
      val response = jacksonObjectMapper().readValue(content, GCSResponse::class.java)

      if (response.nextPageToken.isNullOrBlank()) {
        return response.prefixes
      }
      return response.prefixes + queryGcs(response.nextPageToken)
    }

    return queryGcs("")
  }

  private fun getVersionsFromGCSPrefixes(versions: List<String>): List<BazelVersion> = versions.map { version ->
    version.replace("/", "").removeSuffix("release").let(::BazelVersion)
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private data class GCSResponse(val prefixes: List<String>, val nextPageToken: String?)

  private inline fun ifNotBlank(fromObject: String, transform: (String) -> String): String =
    if (fromObject.isNotBlank()) transform(fromObject) else ""
}
