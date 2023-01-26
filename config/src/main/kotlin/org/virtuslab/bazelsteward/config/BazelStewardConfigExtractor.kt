package org.virtuslab.bazelsteward.config

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Path
import kotlin.io.path.exists

data class BazelStewardConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val maven: MavenConfig = MavenConfig()
)

data class MavenConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val ruledDependencies: List<MavenDependency> = emptyList()
)

data class MavenDependency(
  val id: MavenLibraryId,
  val versioning: VersioningSchema
)

private val logger = KotlinLogging.logger { }

class VersioningSchemaDeserializer : StdDeserializer<VersioningSchema>(VersioningSchema::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): VersioningSchema {
    val versioningFieldValue = (jp.codec.readTree<JsonNode>(jp) as TextNode).asText().toString()
    return VersioningSchema(versioningFieldValue)
  }
}

class BazelStewardConfigExtractor(repoRoot: Path) {

  private val configFilePath = repoRoot.resolve(".bazel-steward.yaml")

  suspend fun get(): BazelStewardConfig {

    return withContext(Dispatchers.IO) {
      val schemaContent = javaClass.classLoader.getResource("bazel-steward-schema.json")?.readText()
        ?: throw Exception("Could not find schema to validate configuration file")
      val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaContent)

      runCatching {
        if (!configFilePath.exists()) return@withContext BazelStewardConfig()
        val configContent = configFilePath.toFile()
          .readLines()
          .filterNot { it.startsWith("#") }
          .joinToString("\n")
          .ifEmpty { return@withContext BazelStewardConfig() }
        val yamlReader = ObjectMapper(YAMLFactory())
        val kotlinModule = KotlinModule()
        kotlinModule.addDeserializer(VersioningSchema::class.java, VersioningSchemaDeserializer())
        yamlReader.registerModule(kotlinModule)
        val validationResult = schema.validate(yamlReader.readTree(configContent))
        if (validationResult.isNotEmpty()) {
          throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
        } else {
          yamlReader.readValue(configContent, BazelStewardConfig::class.java)
        }
      }.getOrElse {
        logger.error { "Could not parse $configFilePath file!" }
        throw it
      }
    }
  }
}
