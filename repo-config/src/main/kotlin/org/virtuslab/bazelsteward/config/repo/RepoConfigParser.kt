package org.virtuslab.bazelsteward.config.repo

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.networknt.schema.JsonSchema
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
import kotlin.io.path.readText

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

class DependencyNameFilterDeserializer : StdDeserializer<DependencyNameFilter?>(DependencyNameFilter::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): DependencyNameFilter? {
    return (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText()?.toString()?.let { fieldValue ->
      DependencyNameFilter.parse(fieldValue)
    }
  }
}

class RepoConfigParser {

  private val schema = loadSchema()

  suspend fun load(path: Path): RepoConfig {
    return withContext(Dispatchers.IO) {
      runCatching {
        if (!path.exists()) return@withContext RepoConfig()
        return@withContext parse(path.readText())
      }.getOrElse {
        logger.error { "Could not parse $path file!" }
        throw it
      }
    }
  }

  fun parse(text: String): RepoConfig {
    val configContent = text
      .lines()
      .filterNot { it.startsWith("#") }
      .joinToString("\n")
      .ifEmpty { return RepoConfig() }
    val yamlReader = ObjectMapper(YAMLFactory())
    val kotlinModule = KotlinModule()
    yamlReader.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
    kotlinModule.addDeserializer(VersioningSchema::class.java, VersioningSchemaDeserializer())
    kotlinModule.addDeserializer(PinningStrategy::class.java, PinningStrategyDeserializer())
    kotlinModule.addDeserializer(BumpingStrategy::class.java, BumpingStrategyDeserializer())
    kotlinModule.addDeserializer(DependencyNameFilter::class.java, DependencyNameFilterDeserializer())
    yamlReader.registerModule(kotlinModule)
    val validationResult = schema.validate(yamlReader.readTree(configContent))
    if (validationResult.isNotEmpty()) {
      throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
    } else {
      return yamlReader.readValue(configContent, RepoConfig::class.java)
    }
  }

  private fun loadSchema(): JsonSchema {
    val schemaText = javaClass.classLoader.getResource("repo-config-schema.json")?.readText()
      ?: throw Exception("Could not find schema to validate configuration file")
    return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaText)
  }
}
