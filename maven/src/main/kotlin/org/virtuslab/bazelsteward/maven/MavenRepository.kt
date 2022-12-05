package org.virtuslab.bazelsteward.maven

import coursierapi.Module
import coursierapi.Repository
import coursierapi.Versions
import kotlinx.coroutines.*
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version

class MavenRepository {
  suspend fun findVersions(libraries: List<MavenCoordinates>): Map<MavenCoordinates, List<Version>> =
    withContext(Dispatchers.IO) {
      coroutineScope {
        libraries.map { coordinates ->
          async {
            val versionResult =
              Versions.create().withRepositories(Repository.central())
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
