/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn

/** Candidate days inside one month or year for a rule, applying BYMONTHDAY, BYDAY and the invalid-date policy. */
internal class MonthDays(
    private val calendar: CalendarArithmetic,
    private val rule: RecurrenceRule,
    private val start: CalendarDate,
) {
    fun inMonth(
        year: Int,
        month: Int,
    ): List<Long> {
        val byMonthDay = rule.byMonthDay.mapNotNull { resolve(year, month, it) }
        val byDay = weekdays(daysOf(year, month), rule.byDay)
        return when {
            rule.byMonthDay.isNotEmpty() && rule.byDay.isNotEmpty() -> byMonthDay.intersect(byDay.toSet()).toList()
            rule.byMonthDay.isNotEmpty() -> byMonthDay
            rule.byDay.isNotEmpty() -> byDay
            else -> listOfNotNull(resolve(year, month, start.day))
        }.distinct().sorted()
    }

    fun inYear(year: Int): List<Long> {
        if (rule.byMonthDay.isEmpty() && rule.byDay.isEmpty()) {
            return listOfNotNull(resolve(year, calendar.sameMonthIn(start.year, start.month, year), start.day))
        }
        val months = 1..calendar.monthsInYear(year)
        val byMonthDay = months.flatMap { month -> rule.byMonthDay.mapNotNull { resolve(year, month, it) } }
        val byDay = weekdays(months.flatMap { daysOf(year, it) }, rule.byDay)
        return when {
            rule.byMonthDay.isNotEmpty() && rule.byDay.isNotEmpty() -> byMonthDay.intersect(byDay.toSet()).toList()
            rule.byMonthDay.isNotEmpty() -> byMonthDay
            else -> byDay
        }.distinct().sorted()
    }

    private fun daysOf(
        year: Int,
        month: Int,
    ): List<Long> {
        val first = calendar.toJdn(calendar.date(year, month, 1)).value
        return (0 until calendar.monthLength(year, month)).map { first + it }
    }

    /** Days of [span] matching any of [byDay]; an ordinal counts within the span (from its end when negative). */
    private fun weekdays(
        span: List<Long>,
        byDay: List<WeekdayNum>,
    ): List<Long> =
        byDay.flatMap { entry ->
            val matching = span.filter { Jdn(it).weekday() == entry.weekday }
            when (val ordinal = entry.ordinal) {
                null -> matching
                else -> listOfNotNull(matching.getOrNull(if (ordinal > 0) ordinal - 1 else matching.size + ordinal))
            }
        }

    /** The day [day] (negative: from the month's end) of [year]-[month], or the policy's replacement. */
    private fun resolve(
        year: Int,
        month: Int,
        day: Int,
    ): Long? {
        val months = calendar.monthsInYear(year)
        if (month !in 1..months) return missingMonth(year, months)
        val length = calendar.monthLength(year, month)
        val target = if (day > 0) day else length + day + 1
        val first = calendar.toJdn(calendar.date(year, month, 1)).value
        return when {
            target in 1..length -> first + target - 1
            rule.invalidDates == InvalidDatePolicy.SKIP -> null
            target < 1 -> first
            rule.invalidDates == InvalidDatePolicy.NEXT_DAY -> first + length
            else -> first + length - 1
        }
    }

    /** The policy's replacement for a month [year] does not have: after or on its last day, or none. */
    private fun missingMonth(
        year: Int,
        months: Int,
    ): Long? {
        val length = calendar.monthLength(year, months)
        val last = calendar.toJdn(calendar.date(year, months, length)).value
        return when (rule.invalidDates) {
            InvalidDatePolicy.SKIP -> null
            InvalidDatePolicy.NEXT_DAY -> last + 1
            InvalidDatePolicy.LAST_DAY_OF_MONTH -> last
        }
    }
}
