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
        this.prerelease.compareTo(other.prerelease)
    else 0

    companion object {
        private val semVerRegex =
            Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""")

        fun fromString(value: String): Option<SemanticVersion> {
            return semVerRegex.matchEntire(value).toOption().map {
                val values = it.groupValues
                SemanticVersion(
                    values[1].toInt(),
                    values[2].toInt(),
                    values[3].toInt(),
                    values[4],
                    values[5]
                )
            }
        }
    }

    override fun toSemVer(): Option<SemanticVersion> = Some(this)
}


