/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

/** Where a dependency is used; determines which licenses are acceptable (ADR-0003). */
enum class LicenseScope {
    /** Shipped inside an app artifact. */
    RUNTIME,

    /** Build tooling that is never shipped: annotation/KSP processors, lint checks, build-time tools. */
    BUILD,

    /** Test-only: unit, Robolectric, instrumented, screenshot and benchmark classpaths. */
    TEST,
}

/** A license as declared in a POM, with its SPDX identifier when it could be normalized. */
data class DeclaredLicense(
    val name: String?,
    val url: String?,
    val spdxId: String?,
)

/** A `group:name:version` Maven coordinate. */
data class ModuleCoordinate(
    val group: String,
    val name: String,
    val version: String,
) {
    override fun toString(): String = "$group:$name:$version"

    companion object {
        private const val COORDINATE_PARTS = 3

        /** Parses `group:name:version`; throws [IllegalArgumentException] for any other shape. */
        fun parse(text: String): ModuleCoordinate {
            val parts = text.split(':')
            require(parts.size == COORDINATE_PARTS && parts.none { it.isBlank() }) {
                "Invalid module coordinate '$text' (expected group:name:version)"
            }
            return ModuleCoordinate(parts[0], parts[1], parts[2])
        }
    }
}

/** One resolved external module with every scope it appears in and its declared licenses. */
data class DependencyLicenseInfo(
    val coordinate: ModuleCoordinate,
    val scopes: Set<LicenseScope>,
    val licenses: List<DeclaredLicense>,
) {
    /** `group:name`, the key used by allow-list overrides. */
    val module: String get() = "${coordinate.group}:${coordinate.name}"
}

/**
 * Manual license assignment for a module whose metadata is missing or ambiguous.
 * [evidence] must link to the upstream license text; [versions] is `*` or one exact version.
 */
data class LicenseOverride(
    val module: String,
    val license: String,
    val evidence: String,
    val versions: String = ANY_VERSION,
) {
    /** True when this override covers [dependency]. */
    fun appliesTo(dependency: DependencyLicenseInfo): Boolean =
        module == dependency.module &&
            (versions == ANY_VERSION || versions == dependency.coordinate.version)

    companion object {
        const val ANY_VERSION = "*"
    }
}

/** Parsed `config/license/allowed-licenses.json`. */
data class AllowList(
    val licenses: Map<String, Set<LicenseScope>>,
    val overrides: List<LicenseOverride>,
) {
    /** True when [spdxId] is allowed in every one of [scopes]. */
    fun permits(
        spdxId: String,
        scopes: Set<LicenseScope>,
    ): Boolean = licenses[spdxId]?.containsAll(scopes) == true
}

/** A dependency rejected by the license gate. */
data class LicenseViolation(
    val dependency: DependencyLicenseInfo,
    val reason: Reason,
) {
    /** Why the dependency was rejected. */
    enum class Reason {
        /** No declared license could be mapped to an SPDX id and no override exists. */
        UNKNOWN_LICENSE,

        /** Every declared license is forbidden or not allowed in one of the dependency's scopes. */
        NOT_ALLOWED,
    }

    /** One-line human-readable description used in the build failure and the report. */
    fun describe(): String {
        val declared =
            dependency.licenses
                .joinToString(", ") { license ->
                    val label = license.name ?: license.url ?: "?"
                    if (license.spdxId == null) "'$label'" else "'$label' (${license.spdxId})"
                }.ifEmpty { "none declared" }
        val scopes = dependency.scopes.sorted().joinToString("+") { it.name.lowercase() }
        return "${dependency.coordinate} [$scopes] $reason — licenses: $declared"
    }
}
