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
      val repositories = runCatching {
        parseJson(dumpRulesToJson(workspaceRoot))
      }.getOrElse { error ->
        logger.warn { "Could not dump Bazel repositories with Bazel; falling back to WORKSPACE parsing. ${error.message}" }
        parseWorkspaceRepositories(workspaceRoot)
      }

      repositories
        .filter { isUserDeclaredHttpArchive(it) }
        .mapNotNull { toRuleLibrary(it) }
        .also { logResult(it) }
    }

  private fun parseWorkspaceRepositories(workspaceRoot: Path): List<Repository> {
    val workspaceFilePath = listOf("WORKSPACE.bzlmod", "WORKSPACE.bazel", "WORKSPACE")
      .map { workspaceRoot.resolve(it) }
      .find { it.exists() } ?: throw RuntimeException("Could not find workspace file in $workspaceRoot")

    val workspaceContent = workspaceFilePath.readText()
    val variables = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*["']([^"']*)["']\s*$""")
      .findAll(workspaceContent)
      .associate { it.groupValues[1] to it.groupValues[2] }

    return Regex("""(?ms)^\s*http_archive\s*\((.*?)^\s*\)""")
      .findAll(workspaceContent)
      .mapNotNull { parseHttpArchive(it.groupValues[1], variables) }
      .toList()
  }

  private fun parseHttpArchive(block: String, variables: Map<String, String>): Repository? {
    val name = findStringAttr(block, "name", variables)
    val sha256 = findStringAttr(block, "sha256", variables)
    val url = findStringAttr(block, "url", variables)
    val urls = findListAttr(block, "urls", variables)
    val stripPrefix = findStringAttr(block, "strip_prefix", variables)

    if (name == null || sha256 == null || (url == null && urls.isEmpty())) {
      return null
    }

    return Repository(
      generator_function = "",
      kind = "http_archive",
      name = name,
      sha256 = sha256,
      url = url,
      urls = urls.takeUnless { it.isEmpty() },
      strip_prefix = stripPrefix,
    )
  }

  private fun findStringAttr(block: String, attr: String, variables: Map<String, String>): String? =
    Regex("""(?m)^\s*$attr\s*=\s*(.+?)(?:,\s*)?$""")
      .find(block)
      ?.groupValues
      ?.get(1)
      ?.let { evaluateStringExpression(it, variables) }

  private fun findListAttr(block: String, attr: String, variables: Map<String, String>): List<String> {
    val listContent = Regex("""(?ms)^\s*$attr\s*=\s*\[(.*?)]""")
      .find(block)
      ?.groupValues
      ?.get(1)
      ?: return emptyList()

    return Regex("""(?s)(?:"[^"]*"|'[^']*')(?:\.format\([^)]*\)|\s*%\s*[A-Za-z_][A-Za-z0-9_]*)?""")
      .findAll(listContent)
      .mapNotNull { evaluateStringExpression(it.value, variables) }
      .toList()
  }

  private fun evaluateStringExpression(expression: String, variables: Map<String, String>): String? {
    val normalized = expression.trim().removeSuffix(",").trim()
    variables[normalized]?.let { return it }

    val formatMatch = Regex("""(?s)^("[^"]*"|'[^']*')\.format\((.*)\)$""")
      .matchEntire(normalized)
    if (formatMatch != null) {
      val template = unquote(formatMatch.groupValues[1])
      val args = formatMatch.groupValues[2]
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { evaluateStringExpression(it, variables) }

      return args.foldIndexed(template) { index, result, arg ->
        result.replaceFirst("{}", arg).replace("{$index}", arg)
      }
    }

    val percentTupleMatch = Regex("""(?s)^("[^"]*"|'[^']*')\s*%\s*\((.*)\)$""")
      .matchEntire(normalized)
    if (percentTupleMatch != null) {
      val template = unquote(percentTupleMatch.groupValues[1])
      val args = percentTupleMatch.groupValues[2]
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { evaluateStringExpression(it, variables) }

      return args.fold(template) { result, arg -> result.replaceFirst("%s", arg) }
    }

    val percentMatch = Regex("""(?s)^("[^"]*"|'[^']*')\s*%\s*([A-Za-z_][A-Za-z0-9_]*)$""")
      .matchEntire(normalized)
    if (percentMatch != null) {
      return variables[percentMatch.groupValues[2]]?.let { unquote(percentMatch.groupValues[1]).replace("%s", it) }
    }

    return Regex("""^"([^"]*)"$""").matchEntire(normalized)?.groupValues?.get(1)
      ?: Regex("""^'([^']*)'$""").matchEntire(normalized)?.groupValues?.get(1)
  }

  private fun unquote(value: String): String = value.substring(1, value.length - 1)

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
    val bazelPath = CommandRunner.runForOutput(workspaceRoot, "bazel", "info", "output_base").trim()
    logger.info { "Bazel output_base for workspace $workspaceRoot: $bazelPath" }
    // solution from https://github.com/bazelbuild/bazel/issues/6377#issuecomment-1237791008
    try {
      CommandRunner.runForOutput(workspaceRoot, "bazel", "build", "@all_external_repositories//:result.json")
    } finally {
      workspaceFilePath.writeText(originalContent)
      deleteFile(tempFileForBzl)
    }
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
