package org.virtuslab.bazelsteward.rules

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.Config
import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.CommandRunner
import org.virtuslab.bazelsteward.core.rules.RuleLibraryId
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

class BazelRulesExtractor(private val config: Config) {

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

  suspend fun extractCurrentRules(bazelFiles: Map<BazelFileSearch.BazelFile, BazelFileSearch.BazelFileType>): List<RuleLibraryId> =
    withContext(Dispatchers.IO) {
      val dumpRepositoriesContent = javaClass.classLoader.getResource("bazel/resources/dump_repositories.bzlignore")?.readText()
        ?: throw RuntimeException("Could not find dump_repositories template, which is required for detecting used bazel repositories")
      val tempFileForBzl = createTempFile(directory = config.path, suffix = ".bzl").toFile()
      tempFileForBzl.appendText(dumpRepositoriesContent)

      val workspaceFilePath = bazelFiles.entries.first { it.value == BazelFileSearch.BazelFileType.Workspace }.key
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
      if (!resultFilePath.exists()) {
        throw RuntimeException("Failed to find a file")
      }
      val result = yamlReader.readValue(resultFilePath.toFile(), object : TypeReference<List<Repository>>() {})
        .filter {
          it.kind == "http_archive" && it.generator_function.isEmpty() && !it.url.isNullOrEmpty() && !it.sha256.isNullOrEmpty()
        }
        .map { RuleLibraryId.from(it.url!!, it.sha256!!) }
      result
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
