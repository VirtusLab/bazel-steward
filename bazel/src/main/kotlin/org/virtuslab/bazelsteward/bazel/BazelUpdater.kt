package org.virtuslab.bazelsteward.bazel

import mu.KotlinLogging
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.core.library.VersioningType

private val logger = KotlinLogging.logger {}

open class BazelUpdater {
  open suspend fun availableVersions(from: BazelVersion): List<BazelVersion> {
    val versionsExtractor = GcsVersionsExtractor()
    val versioning = VersioningSchema(VersioningType.SEMVER.name)
    val bazelSemVer = from.toSemVer(versioning) ?: run { logger.error { "Cannot parse Bazel Version" }; return emptyList() }
    val newerVersionPrefixes = versionsExtractor.getVersionPrefixes().filter { version -> version.toSemVer(versioning)?.let { it > bazelSemVer } ?: false }
    return newerVersionPrefixes.flatMap { versionPrefix -> versionsExtractor.getAllVersions(versionPrefix) }
  }

  companion object {
    object BazelLibraryId : LibraryId {
      override fun associatedStrings(): List<String> = listOf("", "USE_BAZEL_VERSION")

      override val name: String
        get() = "bazel"
    }

    data class BazelLibrary(override val version: Version) : Library<BazelLibraryId> {
      override val id: BazelLibraryId
        get() = BazelLibraryId
    }
  }
}
