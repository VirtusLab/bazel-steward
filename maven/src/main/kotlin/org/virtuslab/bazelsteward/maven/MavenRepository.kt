package org.virtuslab.bazelsteward.maven

import coursierapi.Cache
import coursierapi.Module
import coursierapi.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version
import java.io.File
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.Path

object CoursierCache {
  private val coursierDefaultCacheWorkaround: Boolean by lazy {
    val writableLocation = File(System.getProperty("java.io.tmpdir"), "bazel-steward-coursier")
    System.setProperty("coursierapi.shaded.coursier.cache", writableLocation.toString())
    true
  }

  fun get(path: Path): Cache {
    return createCache().withLocation(path.toFile())
  }

  // The only constructor of Cache always calls ApiHelper.defaultLocation() which eventually tries to set up
  // cache in default location which may not be writeable. Thus, it is not possible to create the instance and
  // customize it further as the constructor throws. As a workaround we set the system property to some writable
  // location
  private fun createCache(): Cache {
    return try {
      Cache.create()
    } catch (e: Exception) {
      coursierDefaultCacheWorkaround
      Cache.create()
    }
  }
}

class MavenRepository {
  suspend fun findVersions(mavenData: MavenData): Map<MavenCoordinates, List<Version>> =
    withContext(Dispatchers.IO) {
      coroutineScope {
        val mavenRepositories = mavenData.repositories.map { coursierapi.MavenRepository.of(it) }
        val cache = CoursierCache.get(Path(System.getProperty("java.io.tmpdir"), "bazel-steward-coursier"))
        mavenData.dependencies.map { coordinates ->
          async {
            val versionResult =
              Versions.create()
                .withRepositories(*mavenRepositories.toTypedArray())
                .withModule(Module.of(coordinates.id.group, coordinates.id.artifact))
                .withCache(cache)
                .versions()
            if (versionResult.errors.isNotEmpty())
              println(versionResult.errors)
            coordinates to versionResult.mergedListings.available.map { SimpleVersion(it) }
          }
        }.awaitAll().toMap()
      }
    }
}
