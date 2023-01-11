package org.virtuslab.bazelsteward.maven

import coursierapi.Module
import coursierapi.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version

class MavenRepository {
  suspend fun findVersions(mavenData: MavenData): Map<MavenCoordinates, List<Version>> =
    withContext(Dispatchers.IO) {
      coroutineScope {
        val mavenRepositories = mavenData.repositories.map { coursierapi.MavenRepository.of(it) }
        mavenData.dependencies.map { coordinates ->
          async {
            val versionResult =
              Versions.create().withRepositories(*mavenRepositories.toTypedArray())
                .withModule(Module.of(coordinates.id.group, coordinates.id.artifact))
                .versions()
            if (versionResult.errors.isNotEmpty())
              println(versionResult.errors)
            coordinates to versionResult.mergedListings.available.map { SimpleVersion(it) }
          }
        }.awaitAll().toMap()
      }
    }
}
