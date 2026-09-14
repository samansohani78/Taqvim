/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

/**
 * A Taqvim release version (docs/RELEASE.md, T-1900): `MAJOR.MINOR.PATCH` with an optional `-beta.N`.
 *
 * `versionCode = MAJOR × 1 000 000 + MINOR × 10 000 + PATCH × 100 + STAGE`, where STAGE is `N` for a beta
 * (1–98) and [FINAL_STAGE] for the final release, so every beta sorts before its final release and every release
 * before the next.
 */
data class TaqvimVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    /** `N` of `-beta.N`, or `null` for a final release. */
    val beta: Int? = null,
) {
    init {
        require(major in 0..MAX_MAJOR) { "MAJOR must be 0–$MAX_MAJOR, was $major" }
        require(minor in 0..MAX_PART) { "MINOR must be 0–$MAX_PART, was $minor" }
        require(patch in 0..MAX_PART) { "PATCH must be 0–$MAX_PART, was $patch" }
        require(beta == null || beta in 1..MAX_BETA) { "beta.N must be 1–$MAX_BETA, was $beta" }
    }

    /** Semantic version shown to users, e.g. `1.0.0-beta.3`. */
    val name: String
        get() = "$major.$minor.$patch" + (beta?.let { "-beta.$it" } ?: "")

    /** Android `versionCode`; strictly increasing with the version. */
    val code: Int
        get() = major * MAJOR_WEIGHT + minor * MINOR_WEIGHT + patch * PATCH_WEIGHT + (beta ?: FINAL_STAGE)

    companion object {
        /** Gradle property that CI sets from the release tag, e.g. `-Ptaqvim.version=v1.2.3-beta.4`. */
        const val PROPERTY: String = "taqvim.version"

        /** Key of the checked-in default in `version.properties`. */
        const val FILE_KEY: String = "version"

        const val FINAL_STAGE: Int = 99
        private const val MAX_MAJOR = 2099
        private const val MAX_PART = 99
        private const val MAX_BETA = 98
        private const val MAJOR_WEIGHT = 1_000_000
        private const val MINOR_WEIGHT = 10_000
        private const val PATCH_WEIGHT = 100

        private val SYNTAX = Regex("""v?(0|[1-9]\d{0,3})\.(0|[1-9]\d?)\.(0|[1-9]\d?)(?:-beta\.([1-9]\d?))?""")

        /** The version written as `[v]X.Y.Z[-beta.N]`, or `null` when [text] is malformed or out of range. */
        fun parseOrNull(text: String): TaqvimVersion? {
            val groups = SYNTAX.matchEntire(text.trim())?.groupValues ?: return null
            val parts = groups.drop(1).map { it.toIntOrNull() }
            val (major, minor, patch) = parts
            val beta = parts[BETA_GROUP]
            val inRange =
                major != null && major <= MAX_MAJOR && minor != null && patch != null &&
                    (beta == null || beta <= MAX_BETA)
            return if (inRange) TaqvimVersion(major, minor, patch, beta) else null
        }

        /** Like [parseOrNull] but fails the build with a message naming the rejected [source]. */
        fun parse(
            text: String,
            source: String,
        ): TaqvimVersion =
            requireNotNull(parseOrNull(text)) {
                "Invalid Taqvim version '$text' from $source; expected vX.Y.Z or vX.Y.Z-beta.N (docs/RELEASE.md)"
            }

        /**
         * The version from the [PROPERTY] Gradle property when it is set (CI passes the release tag), otherwise
         * from the `version=` line of the checked-in `version.properties` [propertiesFile] text.
         */
        fun resolve(
            property: String?,
            propertiesFile: String?,
        ): TaqvimVersion {
            val fromProperty = property?.takeIf { it.isNotBlank() }
            if (fromProperty != null) return parse(fromProperty, "-P$PROPERTY")
            val line =
                propertiesFile
                    ?.lineSequence()
                    ?.map { it.trim() }
                    ?.firstOrNull { it.startsWith("$FILE_KEY=") }
            val value = requireNotNull(line?.substringAfter('=')) { "version.properties has no '$FILE_KEY=' line" }
            return parse(value, "version.properties")
        }

        private const val BETA_GROUP = 3
    }
}
