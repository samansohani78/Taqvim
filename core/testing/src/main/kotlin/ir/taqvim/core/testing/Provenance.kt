/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import kotlinx.datetime.LocalDate

/**
 * Citation header required on every golden fixture (docs/PLAN.md §8.2).
 *
 * A golden fixture starts with `#`-prefixed `key: value` lines followed by the fixture body:
 * ```
 * # source: University of Tehran, Institute of Geophysics — Official Calendar 1404
 * # url: https://example.org/calendar-1404.pdf
 * # retrieved: 2026-09-13
 * # page: 12
 * # reviewer: pending
 * ```
 * `source`, `url`, `retrieved` (ISO date) and `reviewer` are required; `page` and `notes` are optional.
 */
public data class Provenance(
    public val source: String,
    public val url: String,
    public val retrieved: LocalDate,
    public val reviewer: String,
    public val page: String? = null,
    public val notes: String? = null,
) {
    public companion object {
        /** Prefix of every header line. */
        public const val HEADER_PREFIX: String = "#"

        private val REQUIRED_KEYS = listOf("source", "url", "retrieved", "reviewer")
        private val OPTIONAL_KEYS = listOf("page", "notes")
        private val HTTP_URL = Regex("""^https?://\S+$""")

        /** Parses header lines into a [Provenance]; the failure message lists every problem found. */
        public fun parse(headerLines: List<String>): Result<Provenance> {
            val problems = mutableListOf<String>()
            val fields = parseFields(headerLines, problems)
            REQUIRED_KEYS.filter { fields[it].isNullOrBlank() }.forEach { problems += "missing '$it'" }
            fields["url"]?.takeUnless { HTTP_URL.matches(it) }?.let { problems += "url must be an http(s) URL: '$it'" }
            val retrieved = fields["retrieved"]?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() }
            if (fields["retrieved"] != null && retrieved == null) {
                problems += "retrieved must be an ISO-8601 date: '${fields["retrieved"]}'"
            }
            return if (problems.isEmpty() && retrieved != null) {
                Result.success(
                    Provenance(
                        source = fields.getValue("source"),
                        url = fields.getValue("url"),
                        retrieved = retrieved,
                        reviewer = fields.getValue("reviewer"),
                        page = fields["page"],
                        notes = fields["notes"],
                    ),
                )
            } else {
                Result.failure(IllegalArgumentException(problems.joinToString("; ")))
            }
        }

        private fun parseFields(
            headerLines: List<String>,
            problems: MutableList<String>,
        ): Map<String, String> {
            val fields = LinkedHashMap<String, String>()
            headerLines
                .map { it.removePrefix(HEADER_PREFIX).trim() }
                .filter { it.isNotEmpty() }
                .forEach { content ->
                    val separator = content.indexOf(':')
                    val key = if (separator > 0) content.substring(0, separator).trim().lowercase() else ""
                    when {
                        key.isEmpty() -> problems += "malformed header line '$content'"
                        key !in REQUIRED_KEYS + OPTIONAL_KEYS -> problems += "unknown key '$key'"
                        key in fields -> problems += "duplicate key '$key'"
                        else -> fields[key] = content.substring(separator + 1).trim()
                    }
                }
            return fields
        }
    }
}
