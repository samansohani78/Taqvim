/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import groovy.json.JsonSlurper

/**
 * Parses and validates the license allow-list (`config/license/allowed-licenses.json`).
 *
 * Every structural problem is reported as an [IllegalArgumentException] with a JSON-path-like message,
 * so a broken allow-list fails the build loudly instead of silently allowing everything.
 */
object AllowListParser {
    const val SUPPORTED_SCHEMA_VERSION = 1

    private val MODULE_PATTERN = Regex("""^[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+$""")

    private const val ATTRIBUTION_REQUIRED = "required"
    private val ATTRIBUTION_VALUES = listOf(ATTRIBUTION_REQUIRED, "none")

    /** Parses [json] into an [AllowList]. */
    fun parse(json: String): AllowList {
        val parsed =
            runCatching { JsonSlurper().parseText(json) }
                .getOrElse { throw IllegalArgumentException("Allow-list is not valid JSON: ${it.message}", it) }
        val root = parsed as? Map<*, *> ?: throw IllegalArgumentException("Allow-list root must be a JSON object")
        val schemaVersion = root["schemaVersion"]
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported allow-list schemaVersion '$schemaVersion' (expected $SUPPORTED_SCHEMA_VERSION)"
        }
        val licenses = parseLicenses(root["licenses"])
        return AllowList(
            licenses = licenses,
            overrides = parseOverrides(root["overrides"], licenses.keys),
            dataLicenses = parseDataLicenses(root["dataLicenses"]),
        )
    }

    /**
     * Parses the optional `dataLicenses` section: the licenses that bundled data may carry (ADR-0039).
     * `attribution` is `required` or `none`; nothing else is accepted, so a typo cannot silently drop an obligation.
     */
    private fun parseDataLicenses(node: Any?): List<DataLicenseEntry> {
        if (node == null) return emptyList()
        val entries = node as? List<*> ?: throw IllegalArgumentException("'dataLicenses' must be an array")
        val seen = LinkedHashSet<String>()
        return entries.mapIndexed { index, entry ->
            val path = "dataLicenses[$index]"
            val obj = entry as? Map<*, *> ?: throw IllegalArgumentException("$path must be an object")
            val id = (obj["id"] as? String)?.trim().orEmpty()
            require(id.isNotEmpty()) { "$path.id is required" }
            require(seen.add(id)) { "Duplicate data license id '$id'" }
            val attribution = obj["attribution"] as? String
            require(attribution in ATTRIBUTION_VALUES) {
                "$path.attribution must be one of ${ATTRIBUTION_VALUES.joinToString(", ")}"
            }
            DataLicenseEntry(
                id = id,
                attributionRequired = attribution == ATTRIBUTION_REQUIRED,
                note = (obj["note"] as? String)?.trim()?.ifEmpty { null },
            )
        }
    }

    private fun parseLicenses(node: Any?): Map<String, Set<LicenseScope>> {
        val entries = node as? List<*> ?: throw IllegalArgumentException("'licenses' must be an array")
        require(entries.isNotEmpty()) { "'licenses' must not be empty" }
        val result = LinkedHashMap<String, Set<LicenseScope>>()
        entries.forEachIndexed { index, entry ->
            val path = "licenses[$index]"
            val obj = entry as? Map<*, *> ?: throw IllegalArgumentException("$path must be an object")
            val id = (obj["id"] as? String)?.trim().orEmpty()
            require(id.isNotEmpty()) { "$path.id is required" }
            require(id !in result) { "Duplicate license id '$id'" }
            result[id] = parseScopes(obj["scopes"], "$path.scopes")
        }
        return result
    }

    private fun parseScopes(
        node: Any?,
        path: String,
    ): Set<LicenseScope> {
        val values = node as? List<*> ?: throw IllegalArgumentException("$path must be an array")
        require(values.isNotEmpty()) { "$path must not be empty" }
        return values
            .map { raw ->
                LicenseScope.entries.firstOrNull { it.name.equals(raw as? String, ignoreCase = true) }
                    ?: throw IllegalArgumentException("$path contains unknown scope '$raw'")
            }.toSet()
    }

    private fun parseOverrides(
        node: Any?,
        allowedIds: Set<String>,
    ): List<LicenseOverride> {
        if (node == null) return emptyList()
        val entries = node as? List<*> ?: throw IllegalArgumentException("'overrides' must be an array")
        return entries.mapIndexed { index, entry ->
            val path = "overrides[$index]"
            val obj = entry as? Map<*, *> ?: throw IllegalArgumentException("$path must be an object")
            val module = obj["module"] as? String
            val license = obj["license"] as? String
            val evidence = obj["evidence"] as? String
            require(module != null && MODULE_PATTERN.matches(module)) { "$path.module must be 'group:name'" }
            require(license != null && license in allowedIds) {
                "$path.license '$license' is not an allowed license id"
            }
            require(evidence != null && evidence.startsWith("https://")) {
                "$path.evidence must be an https:// URL to the license text"
            }
            val versions = (obj["versions"] as? String)?.trim()?.ifEmpty { null } ?: LicenseOverride.ANY_VERSION
            LicenseOverride(module = module, license = license, evidence = evidence, versions = versions)
        }
    }
}
