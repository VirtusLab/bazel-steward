package org.virtuslab.bazelsteward.rules

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.common.BazelFileSearch
import org.virtuslab.bazelsteward.common.CommandRunner
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Date
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

class BazelRulesExtractor(private val config: Config) {

  private val yamlReader: ObjectMapper by lazy {
    val objectMapper = ObjectMapper(YAMLFactory())
    val kotlinModule = KotlinModule()
    objectMapper.registerModule(kotlinModule)
    objectMapper
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy ::class)
  data class Asset(
    val name: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: Date?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val updatedAt: Date?,
    val browserDownloadUrl: String?,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy ::class)
  data class ReleaseInfo(
    val body: String?,
    val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: Date?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val publishedAt: Date?,
    val assets: List<Asset>?,
  )

  fun extractLatestRules(currentRules: List<BazelRuleLibraryId>): Map<BazelRuleLibraryId, SimpleVersion> {
    return currentRules.associateWith {
      val client = HttpClient.newBuilder().build()
      // need to add token
      val request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.github.com/repos/${it.repoName}/${it.ruleName}/releases?per_page=1000")) // high value of per_page in order to not send req for next page
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofString())
      val releases = yamlReader.readValue(response.body(), object : TypeReference<List<ReleaseInfo>>() {})
      // - czasami zamiast url jest urls z dwoma url'ami
      // - sha256 w body może być podane jako sha-256 (tzn tylko na takie sie natknęłam, ale pewnie by się jakies repo znalazło co robi np. sha 256)
      // 1. sha256 z body, bierzemy tag_name i podmieniamy w tym url co jest użyty w pliku WORKSPACE i sprawdzamy czy pobieranie działa
      // 2. jeżeli 1 nie działa to sprawdzamy czy w body jest URL
      // 3. jeżeli 2 nie działa to szukamy browser_download_url (w assetach) i próbujemy podmienić
      // jak 3. nie działa to na ten moment kończymy
      SimpleVersion("111")
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Repository(
    val generator_function: String,
    val kind: String?,
    val name: String?,
    val sha256: String?,
    val url: String?,
    val strip_prefix: String?,
  )

  suspend fun extractCurrentRules(bazelFiles: Map<BazelFileSearch.BazelFile, BazelFileSearch.BazelFileType>): List<BazelRuleLibraryId> {
    return withContext(Dispatchers.IO) {
      val dumpRepositoriesContent = javaClass.classLoader.getResource("bazel/resources/dump_repositories.bzlignore")?.readText()
        ?: throw RuntimeException("Could not find dump_repositories, which is required for detecting used bazel repositories")
      val tempFileForBzl = createTempFile(directory = config.path, suffix = ".bzl").toFile()
      tempFileForBzl.appendText(dumpRepositoriesContent)

      val workspaceFilePath = bazelFiles.entries.first { it.value == BazelFileSearch.BazelFileType.WORKSPACE }.key
      val originalContent = workspaceFilePath.content
      workspaceFilePath.path.toFile().appendText(
        """
        |load("${tempFileForBzl.name}", "dump_all_repositories", "repositories_as_json")
        |dump_all_repositories(
        |    name = "all_external_repositories",
        |    repositories_json = repositories_as_json()
        |)""".trimMargin()
      )
      // solution from https://github.com/bazelbuild/bazel/issues/6377#issuecomment-1237791008
      CommandRunner.run("bazel build @all_external_repositories//:result.json".split(' '), config.path.toFile())
      workspaceFilePath.path.toFile().writeText(originalContent)
      deleteFile(tempFileForBzl)
      val bazelPath = CommandRunner.run("bazel info output_base".split(' '), config.path.toFile()).removeSuffix("\n")
      val resultFilePath = Path(bazelPath).resolve("external/all_external_repositories/result.json")
      if (resultFilePath.exists()) {
        yamlReader.readValue(resultFilePath.toFile(), object : TypeReference<List<Repository>>() {})
          .filter {
            it.kind == "http_archive" && it.generator_function.isEmpty() && !it.url.isNullOrEmpty() && !it.sha256.isNullOrEmpty()
          }
          .map { BazelRuleLibraryId(it.url!!, it.sha256!!) }
      } else {
        throw RuntimeException("Failed to find a file")
      }
    }
  }

  private fun deleteFile(file: File) {
    runCatching {
      if (!file.delete()) throw RuntimeException("Deletion fail")
    }.getOrElse {
      logger.error { "Could not delete temporarily added .bzl file " }
      throw it
    }
  }
}
