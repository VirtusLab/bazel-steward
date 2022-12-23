package org.virtuslab.bazelsteward.maven

import coursierapi.Module
import coursierapi.Repository
import coursierapi.Versions
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version

class MavenRepository {
  fun findVersions(libraries: List<MavenCoordinates>): Map<MavenCoordinates, List<Version>> =
    libraries.associateWith { coordinates ->
      val versionResult =
        Versions.create().withRepositories(Repository.central())
          .withModule(Module.of(coordinates.id.group, coordinates.id.artifact))
          .versions()
      if (versionResult.errors.isNotEmpty())
        println(versionResult.errors)
      versionResult.mergedListings.available.map { SimpleVersion(it) }
    }
}
