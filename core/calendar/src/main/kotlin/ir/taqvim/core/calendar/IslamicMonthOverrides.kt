/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Where one overridden month start was published. */
public data class OverrideCitation(
    public val url: String,
    public val title: String,
    public val page: String,
    public val retrieved: String,
)

/** Why an override file was rejected; the app shows a message for each kind and keeps the computed calendar. */
public enum class OverrideProblem {
    /** Not JSON, or not an object with a `months` array. */
    NOT_JSON,

    /** `schemaVersion` is missing or not 1. */
    UNSUPPORTED_VERSION,

    /** Fewer than two month entries (the last entry only marks where the covered range ends). */
    TOO_FEW_MONTHS,

    /** A month entry lacks a field, has an impossible date, or is not the month after the previous one. */
    INVALID_MONTH,

    /** Two consecutive starts are not 29 or 30 days apart. */
    INVALID_LENGTH,

    /** A month lacks its source (url, title, page, retrieval date). */
    MISSING_CITATION,
}

/** An override file could not be used. */
public class InvalidOverrideException(
    public val problem: OverrideProblem,
    detail: String,
) : IllegalArgumentException("$problem: $detail")

/**
 * Optional official lunar Hijri month starts (ADR-0037), e.g. the Iranian Calendar Center's announcements. Never
 * required: without an override every Islamic date is computed. [table] covers the months of the file (the last entry
 * marks the start of the month after the covered range); [citations] has one source per covered month.
 */
public class IslamicMonthOverrides(
    public val table: IslamicMonthTable,
    public val citations: List<OverrideCitation>,
) {
    /** First covered year and month. */
    public val first: Pair<Int, Int>
        get() = table.firstYear to table.firstMonth

    /** Last covered year and month. */
    public val last: Pair<Int, Int>
        get() = table.yearMonthAt(table.monthCount - 1)

    public companion object {
        private const val SCHEMA_VERSION = 1
        private const val MONTHS_PER_YEAR = 12

        // Absolute: R8 repackages the classes of a release build, so a relative resource name misses the file.
        private const val BUNDLED_IRAN = "/ir/taqvim/core/calendar/islamic-iran-official.json"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parses an override file in the D-07 format (`dataset/islamic-iran-overrides.v1.json`): `schemaVersion` 1 and
         * consecutive `months`, each with `hijriYear`, `hijriMonth`, `persianStart` {year, month, day} and a `citation`.
         * Fails with [InvalidOverrideException]; never throws.
         */
        public fun parse(text: String): Result<IslamicMonthOverrides> =
            runCatching { build(root(text)) }.recoverCatching { error ->
                throw error as? InvalidOverrideException ?: InvalidOverrideException(
                    OverrideProblem.NOT_JSON,
                    error.message.orEmpty(),
                )
            }

        /**
         * The official Iranian months bundled with the app (a copy of `dataset/iran/islamic-iran-overrides.json`), for
         * users who switch on "use official announced dates"; the text of that file, or `null` if it is missing.
         */
        public fun bundledIranOfficialText(): String? =
            IslamicMonthOverrides::class.java
                .getResourceAsStream(BUNDLED_IRAN)
                ?.use { it.readBytes().decodeToString() }

        private fun root(text: String): JsonObject {
            val element = json.parseToJsonElement(text)
            val root = element as? JsonObject ?: fail(OverrideProblem.NOT_JSON, "top level is not an object")
            val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
            if (version != SCHEMA_VERSION) fail(OverrideProblem.UNSUPPORTED_VERSION, "schemaVersion $version")
            return root
        }

        private fun build(root: JsonObject): IslamicMonthOverrides {
            val entries = root["months"]?.jsonArray?.map { month(it) }
            if (entries == null) fail(OverrideProblem.NOT_JSON, "no months array")
            if (entries.size < 2) fail(OverrideProblem.TOO_FEW_MONTHS, "${entries.size} month(s)")
            entries.zipWithNext().forEach { (previous, next) -> checkNext(previous, next) }
            val lengths = entries.zipWithNext { previous, next -> (next.startJdn - previous.startJdn).toInt() }
            val first = entries.first()
            return IslamicMonthOverrides(
                IslamicMonthTable(first.year, first.month, first.startJdn, lengths),
                entries.dropLast(1).map { it.citation },
            )
        }

        private fun checkNext(
            previous: Entry,
            next: Entry,
        ) {
            val expected = previous.year * MONTHS_PER_YEAR + previous.month
            if (next.year * MONTHS_PER_YEAR + next.month - 1 != expected) {
                fail(OverrideProblem.INVALID_MONTH, "${next.year}-${next.month} does not follow ${previous.year}")
            }
            if (next.startJdn - previous.startJdn !in SHORT_MONTH..LONG_MONTH) {
                fail(OverrideProblem.INVALID_LENGTH, "month ${previous.year}-${previous.month}")
            }
        }

        private fun month(element: JsonElement): Entry {
            val month = element.jsonObject
            val year = month.int("hijriYear")
            val number = month.int("hijriMonth")?.takeIf { it in 1..MONTHS_PER_YEAR }
            val start = month["persianStart"] as? JsonObject
            val citation = month["citation"] as? JsonObject
            if (year == null || number == null || start == null) {
                fail(OverrideProblem.INVALID_MONTH, month.toString())
            }
            return Entry(year, number, persianJdn(start), citation(citation))
        }

        private fun persianJdn(start: JsonObject): Long {
            val year = start.int("year")
            val month = start.int("month")
            val day = start.int("day")
            if (year == null || month == null || day == null) fail(OverrideProblem.INVALID_MONTH, start.toString())
            val valid =
                month in 1..MONTHS_PER_YEAR && day >= 1 && day <= PersianCalendarSystem.monthLength(year, month)
            if (!valid) fail(OverrideProblem.INVALID_MONTH, start.toString())
            return PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day)).value
        }

        private fun citation(citation: JsonObject?): OverrideCitation {
            val fields = listOf("url", "title", "page", "retrieved").map { citation?.string(it).orEmpty() }
            if (fields.any { it.isBlank() }) fail(OverrideProblem.MISSING_CITATION, citation.toString())
            return OverrideCitation(fields[0], fields[1], fields[2], fields[CITATION_RETRIEVED])
        }

        private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun fail(
            problem: OverrideProblem,
            detail: String,
        ): Nothing = throw InvalidOverrideException(problem, detail)

        private const val SHORT_MONTH = 29L
        private const val LONG_MONTH = 30L
        private const val CITATION_RETRIEVED = 3
    }

    private class Entry(
        val year: Int,
        val month: Int,
        val startJdn: Long,
        val citation: OverrideCitation,
    )
}
