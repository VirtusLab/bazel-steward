package org.virtuslab.bazelsteward.bzlmod

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

private val logger = KotlinLogging.logger {}

open class BzlModRepository {
  private val jsonReader: ObjectMapper by lazy {
    ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }
  }

  open suspend fun findVersions(data: BzlModData): Map<BazelModule, List<Version>> =
    withContext(Dispatchers.IO) {
      val client = HttpClient.newHttpClient()
      coroutineScope {
        data.dependencies.map { dependency ->
          val versions = data.repositories.map { repository ->
            async {
              val uri = URI.create("$repository/modules/${dependency.id.name}/metadata.json")
              val request = HttpRequest.newBuilder(uri).GET().build()
              val response = client.send(request, BodyHandlers.ofString())
              if (response.statusCode() != 200) {
                emptyList()
              } else {
                val tree = jsonReader.readTree(response.body())
                val versions = tree.path("versions")
                if (!versions.isArray) {
                  emptyList()
                } else {
                  val yankedVersions = runCatching {
                    tree.path("yanked_versions").fieldNames().asSequence().toList()
                  }.getOrElse { emptyList() }
                  versions.map { it.asText() }.filterNot(yankedVersions::contains)
                }
              }
            }
          }.awaitAll().flatten()
          if (versions.isEmpty()) {
            logger.error { "Could not find versions for ${dependency.id.name}" }
          }
          dependency to versions.map { SimpleVersion(it) }
        }.toMap()
      }
    }
}
