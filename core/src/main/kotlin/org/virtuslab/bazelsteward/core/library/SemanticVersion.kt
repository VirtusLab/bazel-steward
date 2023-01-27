package org.virtuslab.bazelsteward.core.library

data class SemanticVersion(
  val major: Int,
  val minor: Int,
  val patch: Int,
  val prerelease: String,
  val buildmetadata: String,
) : Version, Comparable<SemanticVersion> {

  override val value: String =
    StringBuilder("$major.$minor.$patch").apply {
      if (prerelease.isNotBlank())
        append("-$prerelease")
      if (buildmetadata.isNotBlank())
        append("+$buildmetadata")
    }.toString()

  override fun compareTo(other: SemanticVersion): Int = if (this.major != other.major)
    this.major.compareTo(other.major)
  else if (this.minor != other.minor)
    this.minor.compareTo(other.minor)
  else if (this.patch != other.patch)
    this.patch.compareTo(other.patch)
  else if (this.prerelease != other.prerelease)
    this.comparePreReleases(this.prerelease, other.prerelease)
  else 0

  private fun comparePreReleases(first: String, second: String): Int {
    // "alpha" < "beta" < "milestone" < "rc" = "cr" < "snapshot" < "" = "final" = "ga" < "sp"
    val qualifiers: Map<String, Int> = mapOf(
      "alpha" to 1,
      "beta" to 2,
      "milestone" to 3,
      "rc" to 4,
      "cr" to 4,
      "snapshot" to 5,
      "ga" to 6,
      "final" to 6,
      "release" to 6,
      "sp" to 7,
      "" to 6,
    )

    val firstQualifier: String? = getQualifierForPreRelease(qualifiers, first)
    val otherQualifier: String? = getQualifierForPreRelease(qualifiers, second)

    return when {
      qualifiers[firstQualifier] == null -> if (qualifiers[otherQualifier] == null) first.compareTo(second) else -1
      qualifiers[otherQualifier] == null -> 1
      else -> qualifiers[firstQualifier]!!.compareTo(qualifiers[otherQualifier]!!)
    }.let {
      if (it == 0) {
        val firstWithoutQualifier = getPreReleaseWithoutQualifier(firstQualifier, first)
        val secondWithoutQualifier = getPreReleaseWithoutQualifier(otherQualifier, second)
        firstWithoutQualifier.compareTo(secondWithoutQualifier)
      } else it
    }
  }

  private fun getQualifierForPreRelease(qualifiers: Map<String, Int>, preRelease: String): String? =
    when {
      preRelease.startsWith('a') -> "alpha"
      preRelease.startsWith('b') -> "beta"
      preRelease.startsWith('m') -> "milestone"
      else -> qualifiers.keys.firstOrNull { preRelease.contains(it) }
    }

  private fun getPreReleaseWithoutQualifier(qualifier: String?, preRelease: String): String =
    qualifier?.let {
      if (preRelease.contains(it)) {
        preRelease.replaceFirst(it, "")
      } else {
        preRelease.replaceFirst(it.first().toString(), "")
      }
    } ?: preRelease

  companion object {
    private val strictSemVerRegex =
      Regex("""^(?<major>0|[1-9]\d*)\.(?<minor>0|[1-9]\d*)\.(?<patch>0|[1-9]\d*)(?:-(?<preRelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?<buildMetaData>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""")
    private val looseSemVerRegex =
      Regex("""^(?<major>0|[1-9]\d*)(?:[.-](?<minor>(0|[1-9]\d*)))?(?:[.-]?(?<patch>(0|[1-9]\d*)))?(?:[-.]?(?<preRelease>((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)))?(?:\+(?<buildMetaData>([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*)))?${'$'}""")

    fun fromString(value: String, versioningScheme: VersioningSchema): SemanticVersion? {
      fun matchToSemanticVersion(regex: Regex): SemanticVersion? {
        return regex.matchEntire(value)?.let { matchResult ->
          val values = matchResult.groups as MatchNamedGroupCollection
          SemanticVersion(
            values["major"]?.value?.toIntOrNull() ?: 0,
            values["minor"]?.value?.toIntOrNull() ?: 0,
            values["patch"]?.value?.toIntOrNull() ?: 0,
            values["preRelease"]?.value.orEmpty(),
            values["buildMetaData"]?.value.orEmpty(),
          )
        }
      }
      return when (versioningScheme) {
        is VersioningSchema.SemVer -> matchToSemanticVersion(strictSemVerRegex)
        is VersioningSchema.Loose -> matchToSemanticVersion(looseSemVerRegex)
        is VersioningSchema.Regex -> matchToSemanticVersion(versioningScheme.regex)
      }
    }
  }

  override fun toSemVer(versioning: VersioningSchema): SemanticVersion = this
}
