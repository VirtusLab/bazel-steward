package org.virtuslab.bazelsteward.config.repo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.HookRunFor
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.GroupId
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

private val logger = KotlinLogging.logger { }

class RepoConfigParser {

  companion object {
    private val pathCandidates = listOf(
      ".bazel-steward.yaml",
      "bazel-steward.yaml",
      ".bazel-steward.yml",
      "bazel-steward.yml",
    )
  }

  private val schema = loadSchema()

  suspend fun load(path: Path?, repositoryRoot: Path, noInternalConfig: Boolean): RepoConfig {
    val defaultPaths = pathCandidates.map { repositoryRoot.resolve(it) }
    val userConfigPath = path ?: defaultPaths.firstOrNull { it.exists() }
    if (noInternalConfig) {
      return loadFromPath(userConfigPath)
    } else {
      val internalConfigContent = javaClass.classLoader.getResource("internal-config.yaml")?.readText()
        ?: throw RuntimeException("Could not find internal-config.yaml file in resources")
      val internalConfig = parse(internalConfigContent)
      if (userConfigPath == null) return internalConfig
      val userConfig = loadFromPath(userConfigPath)
      return userConfig.withFallback(internalConfig)
    }
  }

  suspend fun loadFromPath(path: Path?): RepoConfig {
    return withContext(Dispatchers.IO) {
      runCatching {
        if (path?.exists() == true) {
          logger.info { "Loading configuration from $path" }
          return@withContext parse(path.readText())
        } else {
          return@withContext RepoConfig()
        }
      }.getOrElse {
        logger.error { "Could not parse $path file." }
        throw it
      }
    }
  }

  fun parse(text: String): RepoConfig {
    val configContent = removeComments(text).ifEmpty { return RepoConfig() }
    val yamlMapper = configureObjectMapper()
    val validationResult = schema.validate(yamlMapper.readTree(configContent))
    if (validationResult.isEmpty()) {
      return yamlMapper.readValue(configContent, RepoConfig::class.java)
    } else {
      throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
    }
  }

  private fun loadSchema(): JsonSchema {
    val schemaText = javaClass.classLoader.getResource("repo-config-schema.json")?.readText()
      ?: throw Exception("Could not find schema to validate configuration file")
    return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaText)
  }

  private fun removeComments(text: String) = text
    .lines()
    .filterNot { it.trim().startsWith("#") }
    .joinToString(System.lineSeparator())
    .trim()

  private fun configureObjectMapper(): ObjectMapper {
    val kotlinModule = KotlinModule.Builder().build().apply {
      addDeserializer(VersioningSchema::class.java, VersioningSchemaDeserializer())
      addDeserializer(PinningStrategy::class.java, PinningStrategyDeserializer())
      addDeserializer(BumpingStrategy::class.java, BumpingStrategyDeserializer())
      addDeserializer(DependencyNameFilter::class.java, DependencyNameFilterDeserializer())
      addDeserializer(PathPattern::class.java, PathPatternDeserializer())
      addDeserializer(GroupId::class.java, GroupIdDeserializer())
      addDeserializer(HookRunFor::class.java, HookRunForDeserializer())
    }
    return ObjectMapper(YAMLFactory()).apply {
      propertyNamingStrategy = PropertyNamingStrategies.KEBAB_CASE
      registerModule(kotlinModule)
    }
  }
}
