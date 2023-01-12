package config.src.main.kotlin.org.virtuslab.bazelsteward.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import java.nio.file.Files
import java.nio.file.Path

data class Configuration(
  val maven: MavenConfig?
)

data class MavenConfig(
  val ruledDependencies: List<MavenDependency>?
)

data class MavenDependency(
  val id: MavenLibraryId,
  val versioning: String
)

class BazelStewardConfiguration(repoRoot: Path) {

  private val configFilePath = repoRoot.resolve(".bazel-steward.conf")

  suspend fun get(): Configuration? {

    return withContext(Dispatchers.IO) {
      val schemaContent = this::class.java.classLoader.getResource("bazel-steward-schema.json")?.readText() ?: return@withContext null
      val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaContent)

      runCatching {
        Files.newBufferedReader(configFilePath).use { bufferedReader ->
          val configContent =
            bufferedReader.use { br -> br.readLines().filterNot { it.startsWith("#") }.joinToString("\n") }

          val yamlReader = ObjectMapper(YAMLFactory())
          yamlReader.registerModule(KotlinModule())
          val validationResult = schema.validate(yamlReader.readTree(configContent))
          if (validationResult.isNotEmpty()) {
            throw Exception(validationResult.joinToString(System.lineSeparator()) { it.message.removePrefix("$.") })
          } else {
            yamlReader.readValue(configContent, Configuration::class.java)
          }
        }
      }.onFailure {
        println("Could not parse $configFilePath file!")
        println(it.message)
      }.getOrNull()
    }
  }
}
