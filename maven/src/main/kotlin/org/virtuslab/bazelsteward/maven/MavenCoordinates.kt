package org.virtuslab.bazelsteward.maven

import org.virtuslab.bazelsteward.core.Library
import org.virtuslab.bazelsteward.core.Version

data class MavenLibraryId(val artifact: String, val group: String)

data class MavenCoordinates(
    override val id: MavenLibraryId,
    override val version: Version
) : Library<MavenLibraryId> {
    companion object {
        fun of(artifact: String, group: String, version: String): MavenCoordinates {
            return MavenCoordinates(MavenLibraryId(artifact, group), Version(version))
        }
    }
}