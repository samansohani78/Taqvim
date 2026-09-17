/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/** Applies an [AllowList] to resolved dependencies (ADR-0003). */
object LicensePolicy {
    /** Returns every violation, sorted by coordinate. An empty list means the gate passes. */
    fun evaluate(
        dependencies: List<DependencyLicenseInfo>,
        allowList: AllowList,
    ): List<LicenseViolation> =
        dependencies.sortedBy { it.coordinate.toString() }.mapNotNull { evaluate(it, allowList) }

    /**
     * A dependency passes when an override assigns it an allowed license, or when at least one of its
     * declared licenses (dual licensing lets us choose) is allowed in **every** scope it is used in.
     */
    fun evaluate(
        dependency: DependencyLicenseInfo,
        allowList: AllowList,
    ): LicenseViolation? {
        val override = allowList.overrides.firstOrNull { it.appliesTo(dependency) }
        val ids = override?.let { setOf(it.license) } ?: dependency.licenses.mapNotNull { it.spdxId }.toSet()
        return when {
            ids.isEmpty() -> LicenseViolation(dependency, LicenseViolation.Reason.UNKNOWN_LICENSE)
            ids.any { allowList.permits(it, dependency.scopes.map(::asAllowListScope).toSet()) } -> null
            else -> LicenseViolation(dependency, LicenseViolation.Reason.NOT_ALLOWED)
        }
    }

    /** Debug-only dependencies must meet the [LicenseScope.RUNTIME] rules; the allow-list has no separate entry. */
    private fun asAllowListScope(scope: LicenseScope): LicenseScope =
        if (scope == LicenseScope.DEBUG) LicenseScope.RUNTIME else scope

    /** Human-readable gate result written to the report and used as the failure message. */
    fun summary(
        dependencies: List<DependencyLicenseInfo>,
        violations: List<LicenseViolation>,
    ): String =
        buildString {
            appendLine("License gate: ${dependencies.size} external modules scanned, ${violations.size} violation(s).")
            violations.forEach { appendLine("  - ${it.describe()}") }
            if (violations.isNotEmpty()) {
                appendLine("Policy: docs/adr/0003-license-policy.md; allow-list: config/license/allowed-licenses.json")
            }
        }
}

/** JSON encoding of the intermediate coordinate files and the final license report. */
object LicenseJsonCodec {
    /** Encodes `coordinate → scopes` collected by one project. */
    fun encodeCoordinates(entries: Map<String, Set<LicenseScope>>): String =
        JsonOutput.prettyPrint(
            JsonOutput.toJson(
                entries.toSortedMap().map { (coordinate, scopes) ->
                    mapOf("coordinate" to coordinate, "scopes" to scopes.map { it.name }.sorted())
                },
            ),
        )

    /** Inverse of [encodeCoordinates]. */
    fun decodeCoordinates(json: String): Map<String, Set<LicenseScope>> =
        jsonObjects(json).associate { obj ->
            val coordinate = ModuleCoordinate.parse(obj["coordinate"] as? String ?: "").toString()
            coordinate to scopesOf(obj["scopes"])
        }

    /** Encodes the aggregated report (the CI artifact). */
    fun encodeReport(dependencies: List<DependencyLicenseInfo>): String =
        JsonOutput.prettyPrint(
            JsonOutput.toJson(
                dependencies.sortedBy { it.coordinate.toString() }.map { dependency ->
                    mapOf(
                        "coordinate" to dependency.coordinate.toString(),
                        "scopes" to dependency.scopes.map { it.name }.sorted(),
                        "licenses" to
                            dependency.licenses.map { mapOf("name" to it.name, "url" to it.url, "spdx" to it.spdxId) },
                    )
                },
            ),
        )

    /** Inverse of [encodeReport]. */
    fun decodeReport(json: String): List<DependencyLicenseInfo> =
        jsonObjects(json).map { obj ->
            DependencyLicenseInfo(
                coordinate = ModuleCoordinate.parse(obj["coordinate"] as? String ?: ""),
                scopes = scopesOf(obj["scopes"]),
                licenses =
                    (obj["licenses"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map { license ->
                        DeclaredLicense(
                            license["name"] as? String,
                            license["url"] as? String,
                            license["spdx"] as? String,
                        )
                    },
            )
        }

    private fun jsonObjects(json: String): List<Map<*, *>> {
        val root = JsonSlurper().parseText(json) as? List<*> ?: throw IllegalArgumentException("Expected a JSON array")
        return root.map { it as? Map<*, *> ?: throw IllegalArgumentException("Expected JSON objects in the array") }
    }

    private fun scopesOf(node: Any?): Set<LicenseScope> =
        (node as? List<*>).orEmpty().map { LicenseScope.valueOf(it.toString()) }.toSet()
}

/** Decides which Gradle configurations are scanned and with which [LicenseScope]. */
object ConfigurationClassifier {
    /** Resolvable configuration used by the CI negative test to inject a forbidden dependency. */
    const val CANARY_CONFIGURATION = "licenseGateCanaryClasspath"

    private val TEST_ONLY_PROJECTS = setOf(":benchmark", ":konsist", ":core:testing", ":core:ui-testing")
    private val BUILD_ONLY_PROJECTS = setOf(":lint", ":tools:dataset")
    private val TEST_MARKERS = listOf("unittest", "androidtest", "screenshottest", "testfixtures")

    /** Returns the scope for [configurationName] in [projectPath], or `null` when it is not scanned. */
    fun classify(
        projectPath: String,
        configurationName: String,
    ): LicenseScope? {
        val name = configurationName.lowercase()
        return when {
            configurationName == CANARY_CONFIGURATION -> LicenseScope.RUNTIME
            name.endsWith("processorclasspath") || name == "annotationprocessor" -> LicenseScope.BUILD
            !name.endsWith("runtimeclasspath") -> null
            name.startsWith("test") || TEST_MARKERS.any { it in name } -> LicenseScope.TEST
            projectPath in TEST_ONLY_PROJECTS -> LicenseScope.TEST
            projectPath in BUILD_ONLY_PROJECTS -> LicenseScope.BUILD
            name.startsWith("debug") -> LicenseScope.DEBUG
            else -> LicenseScope.RUNTIME
        }
    }
}
