package org.virtuslab.bazelsteward.config

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Path
import kotlin.io.path.exists

data class Configuration(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val maven: MavenConfig = MavenConfig()
)

data class MavenConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val ruledDependencies: List<MavenDependency> = emptyList()
)

data class MavenDependency(
  val id: MavenLibraryId,
  val versioning: String
)

class BazelStewardConfiguration(repoRoot: Path) {

  private val configFilePath = repoRoot.resolve(".bazel-steward.yaml")

  suspend fun get(): Configuration {

    return withContext(Dispatchers.IO) {
      val schemaContent = javaClass.classLoader.getResource("bazel-steward-schema.json")?.readText() ?: return@withContext Configuration()
      val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaContent)

      runCatching {
        if (!configFilePath.exists()) return@withContext Configuration()
        val configContent = configFilePath.toFile()
          .readLines()
          .filterNot { it.startsWith("#") }
          .joinToString("\n")
          .ifEmpty { return@withContext Configuration() }
        val yamlReader = ObjectMapper(YAMLFactory())
        yamlReader.registerModule(KotlinModule())
        val validationResult = schema.validate(yamlReader.readTree(configContent))
        if (validationResult.isNotEmpty()) {
          throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
        } else {
          yamlReader.readValue(configContent, Configuration::class.java)
        }
      }.getOrElse {
        println("Could not parse $configFilePath file!")
        throw it
      }
    }
  }
}
