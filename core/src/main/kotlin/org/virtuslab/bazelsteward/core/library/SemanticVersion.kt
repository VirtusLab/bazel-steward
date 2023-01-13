package org.virtuslab.bazelsteward.core.library

import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption

data class SemanticVersion(
  val major: Int,
  val minor: Int,
  val patch: Int,
  val prerelease: String,
  val buildmetadata: String,
) : Version,
  Comparable<SemanticVersion> {

  override val value: String = run {
    val builder = StringBuilder("$major.$minor.$patch")
    if (prerelease.isNotBlank())
      builder.append("-$prerelease")
    if (buildmetadata.isNotBlank())
      builder.append("+$buildmetadata")
    builder.toString()
  }

  override fun compareTo(other: SemanticVersion): Int = if (this.major != other.major)
    this.major.compareTo(other.major)
  else if (this.minor != other.minor)
    this.minor.compareTo(other.minor)
  else if (this.patch != other.patch)
    this.patch.compareTo(other.patch)
  else if (this.prerelease != other.prerelease)
    this.comparePreReleases(this.prerelease, other.prerelease)
  else 0

  private fun comparePreReleases(preRelease: String, other: String): Int {
    val qualifiers: Map<String, Int> = mapOf(
      Pair("alpha", 1),
      Pair("beta", 2),
      Pair("milestone", 3),
      Pair("rc", 4),
      Pair("cr", 4),
      Pair("snapshot", 5),
      Pair("", 6),
      Pair("ga", 6),
      Pair("final", 6),
      Pair("release", 6),
      Pair("sp", 7)
    )

    val firstQualifier: String? = getQualifierForPreRelease(qualifiers, preRelease)
    val otherQualifier: String? = getQualifierForPreRelease(qualifiers, other)

    val compareQualifiers = when {
      qualifiers[firstQualifier] == null -> if (qualifiers[otherQualifier] == null) preRelease.compareTo(other) else -1
      qualifiers[otherQualifier] == null -> 1
      else -> qualifiers[firstQualifier]!!.compareTo(qualifiers[otherQualifier]!!)
    }.let {
      if (it == 0) {
        val preReleaseNoQualifier = getPreReleaseNoQualifier(firstQualifier!!, preRelease)
        val otherNoQualifier = getPreReleaseNoQualifier(otherQualifier!!, other)
        preReleaseNoQualifier.compareTo(otherNoQualifier)
      } else it
    }

    return compareQualifiers
  }

  private fun getQualifierForPreRelease(qualifiers: Map<String, Int>, preRelease: String): String? {
    return when {
      preRelease.startsWith('a') -> "alpha"
      preRelease.startsWith('b') -> "beta"
      preRelease.startsWith('m') -> "milestone"
      else -> qualifiers.keys.firstOrNull { preRelease.contains(it) }
    }
  }

  private fun getPreReleaseNoQualifier(qualifier: String, preRelease: String): String {
    return if (preRelease.contains(qualifier)) {
      preRelease.replaceFirst(qualifier, "")
    } else {
      preRelease.replaceFirst(qualifier.first().toString(), "")
    }
  }

  companion object {
    private val canonicalSemVerRegex =
      Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""")
    private val semVerRegex =
      Regex("""^(?<majorRegex>0|[1-9]\d*)(?:[.-](?<minorRegex>(0|[1-9]\d*)))?(?:[.-]?(?<patchRegex>(0|[1-9]\d*)?))(?:[-.]?(?<preReleaseRegex>((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)))?(?:\+(?<buildMetaDataRegex>([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*)))?${'$'}""")

    fun fromString(value: String): Option<SemanticVersion> {
      return semVerRegex.matchEntire(value).toOption().map { matchResult ->
        val values = matchResult.groups as MatchNamedGroupCollection
        SemanticVersion(
          values["majorRegex"]?.value?.toIntOrNull() ?: 0,
          values["minorRegex"]?.value?.toIntOrNull() ?: 0,
          values["patchRegex"]?.value?.toIntOrNull() ?: 0,
          values["preReleaseRegex"]?.value ?: "",
          values["buildMetaDataRegex"]?.value ?: "",
        )
      }
    }
  }

  override fun toSemVer(): Option<SemanticVersion> = Some(this)
}
