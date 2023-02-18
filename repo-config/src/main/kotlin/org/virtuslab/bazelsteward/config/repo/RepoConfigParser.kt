package org.virtuslab.bazelsteward.config.repo

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
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import java.nio.file.Path
import kotlin.io.path.exists

data class RepoConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val maven: MavenConfig = MavenConfig()
)

data class MavenConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val configs: List<ConfigEntry> = emptyList(),
)

data class ConfigEntry(
  val group: String?,
  val artifact: String?,
  val pin: PinningStrategy?,
  val versioning: VersioningSchema?,
  val bumping: BumpingStrategy?,
)

private val logger = KotlinLogging.logger { }

class VersioningSchemaDeserializer : StdDeserializer<VersioningSchema?>(VersioningSchema::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): VersioningSchema? {
    val versioningFieldValue = (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText().toString()
    if (versioningFieldValue.startsWith("regex:")) {
      return VersioningSchema.Regex(versioningFieldValue.removePrefix("regex:").toRegex())
    }
    return VersioningSchema::class.sealedSubclasses.firstOrNull { it.simpleName?.lowercase() == versioningFieldValue }?.objectInstance
  }
}

class PinningStrategyDeserializer : StdDeserializer<PinningStrategy?>(PinningStrategy::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): PinningStrategy? {
    val pinFieldValue = (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText().toString()
    return PinningStrategy.parse(pinFieldValue)
  }
}

class BumpingStrategyDeserializer : StdDeserializer<BumpingStrategy?>(BumpingStrategy::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): BumpingStrategy? {
    return (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText()?.toString()?.let { fieldValue ->
      val str = fieldValue.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      BumpingStrategy.valueOf(str)
    }
  }
}

class RepoConfigParser(private val configFilePath: Path) {

  suspend fun get(): RepoConfig {

    return withContext(Dispatchers.IO) {
      val schemaContent = javaClass.classLoader.getResource("repo-config-schema.json")?.readText()
        ?: throw Exception("Could not find schema to validate configuration file")
      val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaContent)

      runCatching {
        if (!configFilePath.exists()) return@withContext RepoConfig()
        val configContent = configFilePath.toFile()
          .readLines()
          .filterNot { it.startsWith("#") }
          .joinToString("\n")
          .ifEmpty { return@withContext RepoConfig() }
        val yamlReader = ObjectMapper(YAMLFactory())
        val kotlinModule = KotlinModule()
        kotlinModule.addDeserializer(VersioningSchema::class.java, VersioningSchemaDeserializer())
        kotlinModule.addDeserializer(PinningStrategy::class.java, PinningStrategyDeserializer())
        kotlinModule.addDeserializer(BumpingStrategy::class.java, BumpingStrategyDeserializer())
        yamlReader.registerModule(kotlinModule)
        val validationResult = schema.validate(yamlReader.readTree(configContent))
        if (validationResult.isNotEmpty()) {
          throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
        } else {
          yamlReader.readValue(configContent, RepoConfig::class.java)
        }
      }.getOrElse {
        logger.error { "Could not parse $configFilePath file!" }
        throw it
      }
    }
  }
}
