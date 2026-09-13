/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** One event object of a schema-valid dataset file, found at [location] of [file]. */
internal data class EventRecord(
    val file: String,
    val location: String,
    val event: JsonObject,
) {
    val id: String? get() = event.string("id")
    val calendar: String? get() = event.string("calendar")
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.integer(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

private fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject

/**
 * Cross-record rules that JSON Schema cannot express: unique ids across all files, existing `RelativeToEvent`
 * targets, day ranges per calendar (the longest month of each calendar: Persian 31/30, Islamic 30, Gregorian by month
 * with 29 February, Nepali 32; Islamic years at most 355 days) and validity years.
 */
internal object SemanticChecks {
    private const val PERSIAN_LONG_MONTHS = 6
    private const val PERSIAN_LONG_MONTH_DAYS = 31
    private const val PERSIAN_SHORT_MONTH_DAYS = 30
    private const val ISLAMIC_MONTH_DAYS = 30
    private const val ISLAMIC_YEAR_DAYS = 355
    private const val NEPALI_MONTH_DAYS = 32
    private val GREGORIAN_MONTH_DAYS = listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    fun check(records: List<EventRecord>): List<DatasetIssue> {
        val ids = records.mapNotNull { it.id }.toSet()
        return duplicateIds(records) + records.flatMap { ruleIssues(it, ids) + validityIssues(it) }
    }

    private fun duplicateIds(records: List<EventRecord>): List<DatasetIssue> =
        records
            .filter { it.id != null }
            .groupBy { it.id }
            .values
            .flatMap { sameId ->
                val first = sameId.first()
                sameId.drop(1).map {
                    issue(
                        it,
                        ".id",
                        IssueKind.DUPLICATE_ID,
                        "id '${it.id}' is already used at ${first.file}: ${first.location}",
                    )
                }
            }

    private fun ruleIssues(
        record: EventRecord,
        ids: Set<String>,
    ): List<DatasetIssue> {
        val rule = record.event.child("rule") ?: return emptyList()
        return when (rule.string("type")) {
            "Fixed", "Single" -> dayIssues(record, rule)
            "NthDayOfYear" -> dayOfYearIssues(record, rule)
            "RelativeToEvent" -> referenceIssues(record, rule, ids)
            else -> emptyList()
        }
    }

    private fun dayIssues(
        record: EventRecord,
        rule: JsonObject,
    ): List<DatasetIssue> {
        val month = rule.integer("month")
        val day = rule.integer("day")
        val longest = month?.let { longestMonth(record.calendar, it) }
        if (day == null || longest == null || day <= longest) return emptyList()
        val message = "${record.calendar} month $month has at most $longest days (was $day)"
        return listOf(issue(record, ".rule.day", IssueKind.DAY_OUT_OF_RANGE, message))
    }

    private fun longestMonth(
        calendar: String?,
        month: Int,
    ): Int? =
        when (calendar) {
            "PERSIAN" -> if (month <= PERSIAN_LONG_MONTHS) PERSIAN_LONG_MONTH_DAYS else PERSIAN_SHORT_MONTH_DAYS
            "ISLAMIC" -> ISLAMIC_MONTH_DAYS
            "GREGORIAN" -> GREGORIAN_MONTH_DAYS.getOrNull(month - 1)
            "NEPALI" -> NEPALI_MONTH_DAYS
            else -> null
        }

    private fun dayOfYearIssues(
        record: EventRecord,
        rule: JsonObject,
    ): List<DatasetIssue> {
        val n = rule.integer("n") ?: return emptyList()
        return if (record.calendar == "ISLAMIC" && n > ISLAMIC_YEAR_DAYS) {
            listOf(issue(record, ".rule.n", IssueKind.DAY_OUT_OF_RANGE, "Islamic years have at most 355 days (was $n)"))
        } else {
            emptyList()
        }
    }

    private fun referenceIssues(
        record: EventRecord,
        rule: JsonObject,
        ids: Set<String>,
    ): List<DatasetIssue> {
        val target = rule.string("eventId") ?: return emptyList()
        val message =
            when {
                target == record.id -> "event '$target' cannot be relative to itself"
                target !in ids -> "event '$target' does not exist"
                else -> return emptyList()
            }
        return listOf(issue(record, ".rule.eventId", IssueKind.UNKNOWN_EVENT_REFERENCE, message))
    }

    private fun validityIssues(record: EventRecord): List<DatasetIssue> {
        val validity = record.event.child("validity") ?: return emptyList()
        val from = validity.integer("fromYear")
        val to = validity.integer("toYear")
        val message =
            when {
                from == null && to == null -> "validity needs fromYear or toYear"
                from != null && to != null && from > to -> "fromYear $from is after toYear $to"
                else -> return emptyList()
            }
        return listOf(issue(record, ".validity", IssueKind.INVALID_VALIDITY, message))
    }

    private fun issue(
        record: EventRecord,
        path: String,
        kind: IssueKind,
        message: String,
    ) = DatasetIssue(record.file, record.location + path, kind, message)
}
