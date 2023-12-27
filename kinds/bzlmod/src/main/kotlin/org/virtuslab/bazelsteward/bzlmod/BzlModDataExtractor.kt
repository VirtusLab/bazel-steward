package org.virtuslab.bazelsteward.bzlmod

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.TextFileResolver
import org.virtuslab.bazelsteward.core.common.CommandRunner
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.core.replacement.PythonFunctionCallHeuristic
import java.nio.file.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

class BzlModDataExtractor(
  private val workspaceRoot: Path,
) {
  private val jsonReader: ObjectMapper by lazy {
    ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }
  }
  private val regexPattern = """─(\w+)@(.*)\n""".toRegex()
  private lateinit var textFileResolver: TextFileResolver

  fun setFileResolver(r: TextFileResolver) {
    textFileResolver = r
  }

  suspend fun extract(): BzlModData {
    val dependencies = findDependencies()
    val repositories = findRepositories()
    return BzlModData(repositories, dependencies)
  }

  private fun findRepositories(): List<String> {
    val lockFile = workspaceRoot.resolve("MODULE.bazel.lock")
    val repositories = if (lockFile.exists()) {
      jsonReader.readTree(lockFile.toFile()).path("flags").path("cmdRegistries").map { it.asText() }
    } else {
      listOf("https://bcr.bazel.build")
    }
    return repositories.map { it.removeSuffix("/") }
  }

  private suspend fun findDependencies(): List<BazelModule> = withContext(Dispatchers.IO) {
    val output = CommandRunner.runForOutput(workspaceRoot, "bazel", "mod", "graph", "--depth=1")
    return@withContext regexPattern.findAll(output).mapNotNull {
      val name = it.groups[1]!!.value.trim()
      val version = it.groups[2]!!.value.trim()
      if (version == "_") {
        logger.warn { "Cannot update version for $name. It is probably overridden." }
        null
      } else {
        val library = BazelModule(BazelModuleId(name), SimpleVersion(version))
        val versionFromFile = findVersionFromFiles(library, name)
        if (versionFromFile != null) {
          library.copy(version = versionFromFile)
        } else {
          library
        }
      }
    }.toList()
  }

  private fun findVersionFromFiles(
    library: BazelModule,
    name: String
  ): SimpleVersion? {
    val textFiles = textFileResolver.resolve(library)
    val calls = PythonFunctionCallHeuristic.getFunctionCalls(textFiles)
    return calls
      .map { it.matchedText.replace("\\s+".toRegex(), "") }
      .filter { it.startsWith("bazel_dep(") && it.contains("""name="$name"""") }
      .mapNotNull { """version="([^"]*)"""".toRegex().find(it)?.groupValues?.getOrNull(1) }
      .filter { it.isNotEmpty() }
      .map { SimpleVersion(it) }
      .maxByOrNull { it.toSemVer(VersioningSchema.Loose) ?: SemanticVersion.MIN }
  }
}
