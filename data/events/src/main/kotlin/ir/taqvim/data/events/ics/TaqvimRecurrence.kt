/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/** A recurrence [rule] with the [calendar] its months and years count in (ADR-0011). */
data class CalendarRecurrence(
    val calendar: CalendarSystem,
    val rule: RecurrenceRule,
)

/**
 * The `X-TAQVIM-RECURRENCE` property (ADR-0013): a Taqvim rule in any calendar as `KEY=VALUE` parts joined by `;` —
 * `CALENDAR`, `FREQ`, `INTERVAL`, `COUNT`, `UNTIL` (a Julian day number), `BYDAY` and `WKST` (RFC 5545 weekday codes),
 * `BYMONTHDAY` and `INVALID` (an [InvalidDatePolicy] name).
 */
object TaqvimRecurrence {
    /** The property name. */
    const val PROPERTY: String = "X-TAQVIM-RECURRENCE"

    private val CODES =
        mapOf(
            Weekday.MONDAY to "MO",
            Weekday.TUESDAY to "TU",
            Weekday.WEDNESDAY to "WE",
            Weekday.THURSDAY to "TH",
            Weekday.FRIDAY to "FR",
            Weekday.SATURDAY to "SA",
            Weekday.SUNDAY to "SU",
        )
    private val BY_DAY = Regex("""([+-]?\d{1,2})?(MO|TU|WE|TH|FR|SA|SU)""")

    /** [recurrence] as a property value. */
    fun format(recurrence: CalendarRecurrence): String {
        val rule = recurrence.rule
        return buildList {
            add("CALENDAR=${recurrence.calendar.name}")
            add("FREQ=${rule.frequency.name}")
            add("INTERVAL=${rule.interval}")
            rule.count?.let { add("COUNT=$it") }
            rule.until?.let { add("UNTIL=${it.value}") }
            if (rule.byDay.isNotEmpty()) add("BYDAY=" + rule.byDay.joinToString(",", transform = ::code))
            if (rule.byMonthDay.isNotEmpty()) add("BYMONTHDAY=" + rule.byMonthDay.joinToString(","))
            add("INVALID=${rule.invalidDates.name}")
            add("WKST=${CODES.getValue(rule.weekStart)}")
        }.joinToString(";")
    }

    /** The recurrence written in [text], or `null` when CALENDAR or FREQ is missing or any part is invalid. */
    fun parse(text: String): CalendarRecurrence? =
        runCatching {
            val parts = text.split(';').associate { it.substringBefore('=') to it.substringAfter('=', "") }
            CalendarRecurrence(
                calendar = CalendarSystem.valueOf(parts.getValue("CALENDAR")),
                rule =
                    RecurrenceRule(
                        frequency = Frequency.valueOf(parts.getValue("FREQ")),
                        interval = parts["INTERVAL"]?.toInt() ?: 1,
                        count = parts["COUNT"]?.toInt(),
                        until = parts["UNTIL"]?.toLong()?.let(::Jdn),
                        byDay = parts["BYDAY"]?.split(',')?.map(::weekdayNum).orEmpty(),
                        byMonthDay = parts["BYMONTHDAY"]?.split(',')?.map(String::toInt).orEmpty(),
                        invalidDates = parts["INVALID"]?.let(InvalidDatePolicy::valueOf) ?: InvalidDatePolicy.SKIP,
                        weekStart = parts["WKST"]?.let(::weekday) ?: Weekday.MONDAY,
                    ),
            )
        }.getOrNull()

    private fun code(day: WeekdayNum): String = (day.ordinal?.toString() ?: "") + CODES.getValue(day.weekday)

    private fun weekdayNum(text: String): WeekdayNum {
        val (ordinal, code) = requireNotNull(BY_DAY.matchEntire(text)) { "invalid BYDAY '$text'" }.destructured
        return WeekdayNum(weekday(code), ordinal.takeIf { it.isNotEmpty() }?.toInt())
    }

    private fun weekday(code: String): Weekday =
        requireNotNull(CODES.entries.firstOrNull { it.value == code }?.key) { "invalid weekday '$code'" }
}
