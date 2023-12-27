package org.virtuslab.bazelsteward.bazel.rules

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.common.CommandRunner
import java.io.File
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createTempFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

class BazelRulesExtractor {

  private val jsonReader: ObjectMapper by lazy {
    ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Repository(
    val generator_function: String,
    val kind: String?,
    val name: String?,
    val sha256: String?,
    val url: String?,
    val urls: List<String>?,
    val strip_prefix: String?,
  )

  suspend fun extractCurrentRules(workspaceRoot: Path): List<RuleLibrary> =
    withContext(Dispatchers.IO) {
      val resultFilePath = dumpRulesToJson(workspaceRoot)
      val repositories = parseJson(resultFilePath)

      repositories
        .filter { isUserDeclaredHttpArchive(it) }
        .mapNotNull { toRuleLibrary(it) }
        .also { logResult(it) }
    }

  private suspend fun dumpRulesToJson(workspaceRoot: Path): Path {
    val dumpRepositoriesContent = javaClass.classLoader.getResource("dump_repositories.bzl")?.readText()
      ?: throw RuntimeException("Could not find dump_repositories template, which is required for detecting used bazel repositories")
    val tempFileForBzl = createTempFile(directory = workspaceRoot, suffix = ".bzl").toFile()
    tempFileForBzl.appendText(dumpRepositoriesContent)

    val workspaceFilePath = listOf("WORKSPACE.bzlmod", "WORKSPACE.bazel", "WORKSPACE")
      .map { workspaceRoot.resolve(it) }
      .find { it.exists() } ?: throw RuntimeException("Could not find workspace file in $workspaceRoot")

    val originalContent = workspaceFilePath.readText()
    workspaceFilePath.appendText(
      """
        |load("${tempFileForBzl.name}", "dump_all_repositories", "repositories_as_json")
        |dump_all_repositories(
        |    name = "all_external_repositories",
        |    repositories_json = repositories_as_json()
        |)
      """.trimMargin(),
    )
    // solution from https://github.com/bazelbuild/bazel/issues/6377#issuecomment-1237791008
    CommandRunner.runForOutput(workspaceRoot, "bazel", "build", "@all_external_repositories//:result.json")
    workspaceFilePath.writeText(originalContent)
    deleteFile(tempFileForBzl)
    val bazelPath = CommandRunner.runForOutput(workspaceRoot, "bazel", "info", "output_base").trim()
    val resultFilePath = Path(bazelPath).resolve("external/all_external_repositories/result.json")
    if (!resultFilePath.exists()) {
      throw RuntimeException("Failed to find a file: $resultFilePath")
    }
    return resultFilePath
  }

  private fun parseJson(path: Path): List<Repository> {
    return jsonReader.readTree(path.toFile()).elements().asSequence()
      .mapNotNull {
        try {
          jsonReader.convertValue(it, object : TypeReference<Repository>() {})
        } catch (e: Exception) {
          logger.warn { e.message }
          null
        }
      }.toList()
  }

  private fun isUserDeclaredHttpArchive(it: Repository): Boolean {
    val isUserDeclared = it.generator_function.isEmpty()
    val hasUrl = !it.url.isNullOrEmpty() || !it.urls.isNullOrEmpty()
    val hasChecksum = !it.sha256.isNullOrEmpty()
    return it.kind == "http_archive" && isUserDeclared && hasUrl && hasChecksum
  }

  private fun toRuleLibrary(it: Repository): RuleLibrary? {
    val urls = if (it.url.isNullOrEmpty()) it.urls!! else listOf(it.url)
    val libraryId = urls.firstNotNullOfOrNull { runCatching { RuleLibraryId.from(it) }.getOrNull() }

    if (libraryId == null) {
      logger.warn { "Could not parse any of: $urls. Bazel Steward only supports https://github.com/ URLs." }
      return null
    }

    val ruleVersion = RuleVersion.create(libraryId.downloadUrl, it.sha256!!, libraryId.tag, date = Instant.MIN)
    return RuleLibrary(libraryId, ruleVersion)
  }

  private fun logResult(result: List<RuleLibrary>) {
    logger.info { "Found ${result.size} Bazel Rules. " }
    if (result.isNotEmpty()) {
      logger.info { "Bazel Rules found: ${result.joinToString(separator = ", ") { "${it.id.name}:${it.version.tag}" }}" }
    }
  }

  private fun deleteFile(file: File) {
    runCatching {
      if (!file.delete()) throw RuntimeException("Can't delete file ${file.toPath()}")
    }.getOrElse {
      logger.error { "Could not delete temporarily added .bzl file " }
      throw it
    }
  }
}
