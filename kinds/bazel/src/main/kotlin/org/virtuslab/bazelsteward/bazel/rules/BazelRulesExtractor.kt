package org.virtuslab.bazelsteward.bazel.rules

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.AppConfig
import org.virtuslab.bazelsteward.core.common.CommandRunner
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.rules.RuleLibrary
import org.virtuslab.bazelsteward.core.rules.RuleLibraryId
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class BazelRulesExtractor(private val appConfig: AppConfig) {

  private val yamlReader: ObjectMapper by lazy { ObjectMapper(YAMLFactory()).apply { registerModule(KotlinModule()) } }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Repository(
    val generator_function: String,
    val kind: String?,
    val name: String?,
    val sha256: String?,
    val url: String?,
    val strip_prefix: String?,
  )

  suspend fun extractCurrentRules(workspaceRoot: Path): List<RuleLibrary> =
    withContext(Dispatchers.IO) {
      val dumpRepositoriesContent = javaClass.classLoader.getResource("dump_repositories.bzlignore")?.readText()
        ?: throw RuntimeException("Could not find dump_repositories template, which is required for detecting used bazel repositories")
      val tempFileForBzl = createTempFile(directory = appConfig.workspaceRoot, suffix = ".bzl").toFile()
      tempFileForBzl.appendText(dumpRepositoriesContent)

      val workspaceFilePath = listOf("WORKSPACE.bazel", "WORKSPACE")
        .map { workspaceRoot.resolve(it) }
        .find { it.exists() } ?: throw RuntimeException("Could not find workspace file in $workspaceRoot")

      val originalContent = workspaceFilePath.readText()
      workspaceFilePath.appendText(
        """
        |load("${tempFileForBzl.name}", "dump_all_repositories", "repositories_as_json")
        |dump_all_repositories(
        |    name = "all_external_repositories",
        |    repositories_json = repositories_as_json()
        |)""".trimMargin()
      )
      // solution from https://github.com/bazelbuild/bazel/issues/6377#issuecomment-1237791008
      CommandRunner.run("bazel build @all_external_repositories//:result.json".split(' '), appConfig.workspaceRoot)
      workspaceFilePath.writeText(originalContent)
      deleteFile(tempFileForBzl)
      val bazelPath = CommandRunner.run("bazel info output_base".split(' '), appConfig.workspaceRoot).removeSuffix("\n")
      val resultFilePath = Path(bazelPath).resolve("external/all_external_repositories/result.json")
      if (!resultFilePath.exists()) {
        throw RuntimeException("Failed to find a file")
      }
      val result = yamlReader.readValue(resultFilePath.toFile(), object : TypeReference<List<Repository>>() {})
        .filter {
          it.kind == "http_archive" && it.generator_function.isEmpty() && !it.url.isNullOrEmpty() && !it.sha256.isNullOrEmpty()
        }
        .map { RuleLibraryId.from(it.url!!, it.sha256!!) }
      result.map { RuleLibrary(it, SimpleVersion(it.tag)) }
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
