/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import tools.jackson.databind.json.JsonMapper

/** Kinds of problems in an Islamic Iran override file (D-07). */
enum class OverrideIssueKind {
    /** The file is not a single well-formed JSON document. */
    MALFORMED_JSON,

    /** The document violates `dataset/islamic-iran-overrides.v1.json`. */
    SCHEMA,

    /** Two records give a start for the same Hijri month (within a file or across files). */
    DUPLICATE_MONTH,

    /** A Hijri month inside the covered range has no record. */
    MONTH_GAP,

    /** Two consecutive starts are not 29 or 30 days apart. */
    MONTH_LENGTH,

    /** The Persian start date does not exist (e.g. 30 Esfand of a common year). */
    INVALID_DATE,
}

/** One problem found in [file] at [location] (a JSON path). */
data class OverrideIssue(
    val file: String,
    val location: String,
    val kind: OverrideIssueKind,
    val message: String,
) {
    override fun toString(): String = "$file: $location [$kind] $message"
}

/** A Hijri month start of an override file; the Persian date is not yet checked for existence. */
internal data class OverrideMonth(
    val file: String,
    val location: String,
    val hijriYear: Int,
    val hijriMonth: Int,
    val persianYear: Int,
    val persianMonth: Int,
    val persianDay: Int,
) {
    /** Months since the Hijri epoch, so that consecutive months differ by one. */
    val index: Int get() = hijriYear * MONTHS + hijriMonth - 1

    val label: String get() = "$hijriYear-${hijriMonth.toString().padStart(2, '0')}"

    val persianExists: Boolean get() = PersianCalendarSystem.isValid(persianYear, persianMonth, persianDay)

    /** JDN of the Persian start; only for records whose date exists. */
    fun startJdn(): Long =
        PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, persianYear, persianMonth, persianDay)).value

    companion object {
        const val MONTHS = 12
    }
}

/**
 * Validates Islamic Iran override files (D-07): each file against the JSON Schema [schemaText], then the months of all
 * structurally valid files together with [OverrideChecks]. Never throws for any input text.
 */
class OverridesValidator(
    schemaText: String,
) {
    private val schema: Schema =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaText)
    private val mapper = JsonMapper()

    /** Issues of [files] (file name → JSON text) validated as one table; empty when the table is valid. */
    fun validate(files: Map<String, String>): List<OverrideIssue> {
        val checked = files.map { (name, text) -> checkFile(name, text) }
        val structural = checked.flatMap { it.first }
        val months = checked.filter { it.first.isEmpty() }.flatMap { it.second }
        return structural + OverrideChecks.check(months)
    }

    private fun checkFile(
        name: String,
        text: String,
    ): Pair<List<OverrideIssue>, List<OverrideMonth>> {
        val tree = runCatching { mapper.readTree(text) }.getOrNull()
        val document = runCatching { Json.parseToJsonElement(text) }.getOrNull()
        if (tree == null || document == null) {
            return listOf(OverrideIssue(name, "$", OverrideIssueKind.MALFORMED_JSON, MALFORMED_MESSAGE)) to emptyList()
        }
        val schemaIssues =
            runCatching { schema.validate(tree) }
                .map { errors ->
                    errors.map {
                        OverrideIssue(name, it.instanceLocation.toString(), OverrideIssueKind.SCHEMA, it.message)
                    }
                }.getOrElse {
                    val message = "schema evaluation failed: ${it.message}"
                    listOf(OverrideIssue(name, "$", OverrideIssueKind.SCHEMA, message))
                }
        val entries = ((document as? JsonObject)?.get("months") as? JsonArray).orEmpty()
        val months = entries.mapIndexedNotNull { index, entry -> month(name, "$.months[$index]", entry as? JsonObject) }
        return schemaIssues to months
    }

    private fun month(
        file: String,
        location: String,
        entry: JsonObject?,
    ): OverrideMonth? {
        val hijri = entry?.let(::hijriMonthOf)
        val persian = (entry?.get("persianStart") as? JsonObject)?.let(::persianDateOf)
        return if (hijri == null || persian == null) {
            null
        } else {
            OverrideMonth(file, location, hijri.first, hijri.second, persian.first, persian.second, persian.third)
        }
    }

    private fun hijriMonthOf(entry: JsonObject): Pair<Int, Int>? {
        val year = entry.int("hijriYear")
        val month = entry.int("hijriMonth")
        return if (year == null || month == null) null else year to month
    }

    private fun persianDateOf(start: JsonObject): Triple<Int, Int, Int>? {
        val year = start.int("year")
        val month = start.int("month")
        val day = start.int("day")
        return if (year == null || month == null || day == null) null else Triple(year, month, day)
    }

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private companion object {
        const val MALFORMED_MESSAGE = "not a single well-formed JSON document"
    }
}

/** Cross-record rules of the override table: existing dates, one record per month, no gaps, 29/30-day months. */
internal object OverrideChecks {
    private const val SHORT_MONTH = 29
    private const val LONG_MONTH = 30

    fun check(months: List<OverrideMonth>): List<OverrideIssue> {
        val (existing, missing) = months.partition { it.persianExists }
        val invalidDates =
            missing.map {
                val date = "${it.persianYear}-${it.persianMonth}-${it.persianDay}"
                issue(it, OverrideIssueKind.INVALID_DATE, "Persian date $date does not exist")
            }
        val duplicates =
            existing.groupBy { it.index }.values.filter { it.size > 1 }.flatMap { group ->
                val first = group.first()
                group.drop(1).map {
                    val message = "${it.label} is also given at ${first.file} ${first.location}"
                    issue(it, OverrideIssueKind.DUPLICATE_MONTH, message)
                }
            }
        val ordered = existing.distinctBy { it.index }.sortedBy { it.index }
        return invalidDates + duplicates + ordered.zipWithNext().mapNotNull { (earlier, later) -> step(earlier, later) }
    }

    private fun step(
        earlier: OverrideMonth,
        later: OverrideMonth,
    ): OverrideIssue? {
        val days = later.startJdn() - earlier.startJdn()
        return when {
            later.index != earlier.index + 1 -> {
                issue(later, OverrideIssueKind.MONTH_GAP, "${later.label} follows ${earlier.label}: months are missing")
            }

            days !in SHORT_MONTH..LONG_MONTH -> {
                issue(later, OverrideIssueKind.MONTH_LENGTH, "${earlier.label} would have $days days (29 or 30)")
            }

            else -> {
                null
            }
        }
    }

    private fun issue(
        month: OverrideMonth,
        kind: OverrideIssueKind,
        message: String,
    ) = OverrideIssue(month.file, month.location, kind, message)
}
