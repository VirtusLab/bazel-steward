package org.virtuslab.bazelsteward.maven

import org.virtuslab.bazelsteward.core.Version

class MavenRepository {
    fun findVersions(libraries: List<MavenLibraryId>): Map<MavenLibraryId, List<Version>> {
        return emptyMap()
    }
}