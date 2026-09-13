/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import java.util.TreeSet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** What to do when a rule asks for a day its month does not have (e.g. 30 Esfand in a common year, 31 Mehr). */
public enum class InvalidDatePolicy {
    /** No occurrence in that month or year (RFC 5545 behaviour). */
    SKIP,

    /** The day after the month's last day (30 Esfand → 1 Farvardin). */
    NEXT_DAY,

    /** The month's last day (30 Esfand → 29 Esfand). */
    LAST_DAY_OF_MONTH,
}

/**
 * RRULE-lite for personal events in any calendar system (T-503): the RFC 5545 §3.3.10 parts FREQ, INTERVAL, COUNT,
 * UNTIL (inclusive day), BYDAY and BYMONTHDAY, where months and years are those of the event's calendar.
 */
public data class RecurrenceRule(
    public val frequency: Frequency,
    public val interval: Int = 1,
    public val count: Int? = null,
    public val until: Jdn? = null,
    public val byDay: List<WeekdayNum> = emptyList(),
    public val byMonthDay: List<Int> = emptyList(),
    public val invalidDates: InvalidDatePolicy = InvalidDatePolicy.SKIP,
    public val weekStart: Weekday = Weekday.MONDAY,
) {
    init {
        require(interval >= 1) { "interval must be ≥ 1 (was $interval)" }
        require(count == null || count >= 1) { "count must be ≥ 1 (was $count)" }
        require(count == null || until == null) { "count and until must not both be set" }
        require(byMonthDay.all { it != 0 && it in -MAX_MONTH_DAY..MAX_MONTH_DAY }) { "byMonthDay must be ±1…31" }
    }

    private companion object {
        const val MAX_MONTH_DAY = 31
    }
}

/** The calendar day of a DATE or DATE-TIME value: UTC values in UTC, local values as written. */
internal fun IcsDateTime.calendarDay(): LocalDate =
    when (this) {
        is IcsDateTime.Date -> date
        is IcsDateTime.Floating -> dateTime.date
        is IcsDateTime.Utc -> instant.toLocalDateTime(TimeZone.UTC).date
        is IcsDateTime.Zoned -> dateTime.date
    }

/** The Gregorian [RecurrenceRule] equivalent to this RRULE; UTC and floating UNTIL values keep their calendar day. */
public fun Recurrence.toRecurrenceRule(invalidDates: InvalidDatePolicy = InvalidDatePolicy.SKIP): RecurrenceRule =
    RecurrenceRule(
        frequency = frequency,
        interval = interval,
        count = count,
        until = until?.calendarDay()?.toJdn(),
        byDay = byDay,
        byMonthDay = byMonthDay,
        invalidDates = invalidDates,
    )

/**
 * Expands [RecurrenceRule]s in [calendar] (T-503). The start always counts as the first occurrence (RFC 5545
 * §3.3.10, COUNT); later occurrences are strictly increasing. A rule that matches nothing for [MAX_EMPTY_PERIODS]
 * consecutive periods ends its sequence instead of searching forever.
 */
public class RecurrenceEngine(
    private val calendar: CalendarArithmetic,
) {
    /** Occurrences (as day numbers) of [rule] starting at [start], lazily and in ascending order. */
    public fun occurrences(
        start: CalendarDate,
        rule: RecurrenceRule,
    ): Sequence<Jdn> {
        require(start.system == calendar.system) { "start must be a ${calendar.system} date (was ${start.system})" }
        val first = calendar.toJdn(start)
        val later = periods(start, rule).map(::Jdn).filter { it > first }
        val all = sequenceOf(first) + later
        val bounded = rule.until?.let { until -> all.takeWhile { it <= until } } ?: all
        return rule.count?.let { bounded.take(it) } ?: bounded
    }

    /** Candidate days of all periods, merged so that days pushed into the next period stay in order. */
    private fun periods(
        start: CalendarDate,
        rule: RecurrenceRule,
    ): Sequence<Long> =
        sequence {
            val pending = TreeSet<Long>()
            var emptyPeriods = 0
            var index = 0L
            while (emptyPeriods < MAX_EMPTY_PERIODS) {
                val (periodStart, days) = period(start, rule, index)
                pending.addAll(days)
                emptyPeriods = if (days.isEmpty()) emptyPeriods + 1 else 0
                while (pending.isNotEmpty() && pending.first() < periodStart) {
                    val next = pending.first()
                    pending.remove(next)
                    yield(next)
                }
                index++
            }
            yieldAll(pending)
        }

    /** First day of the [index]-th period and the candidate days in it. */
    private fun period(
        start: CalendarDate,
        rule: RecurrenceRule,
        index: Long,
    ): Pair<Long, List<Long>> {
        val steps = Math.multiplyExact(index, rule.interval.toLong())
        return when (rule.frequency) {
            Frequency.DAILY -> {
                val day = calendar.toJdn(start).value + steps
                day to listOf(day)
            }

            Frequency.WEEKLY -> {
                weekly(start, rule, steps)
            }

            Frequency.MONTHLY -> {
                val (year, month) = monthAfter(start.year, start.month, steps)
                firstDay(year, month) to MonthDays(calendar, rule, start).inMonth(year, month)
            }

            Frequency.YEARLY -> {
                val year = Math.toIntExact(start.year + steps)
                firstDay(year, 1) to MonthDays(calendar, rule, start).inYear(year)
            }
        }
    }

    private fun weekly(
        start: CalendarDate,
        rule: RecurrenceRule,
        steps: Long,
    ): Pair<Long, List<Long>> {
        val first = calendar.toJdn(start)
        val weekBegins = first.value - first.weekday().daysAfter(rule.weekStart) + steps * DAYS_PER_WEEK
        val weekdays = rule.byDay.map { it.weekday }.ifEmpty { listOf(first.weekday()) }
        return weekBegins to weekdays.map { weekBegins + it.daysAfter(rule.weekStart) }.distinct().sorted()
    }

    private fun firstDay(
        year: Int,
        month: Int,
    ): Long = calendar.toJdn(calendar.date(year, month, 1)).value

    /** Year and month [steps] months after [year]-[month], honouring the calendar's months per year. */
    private fun monthAfter(
        year: Int,
        month: Int,
        steps: Long,
    ): Pair<Int, Int> {
        var currentYear = year
        var remaining = steps + month - 1
        while (remaining >= calendar.monthsInYear(currentYear)) {
            remaining -= calendar.monthsInYear(currentYear)
            currentYear = Math.addExact(currentYear, 1)
        }
        return currentYear to (remaining.toInt() + 1)
    }

    private companion object {
        const val MAX_EMPTY_PERIODS = 1_000
        const val DAYS_PER_WEEK = 7L
    }
}

/** Candidate days inside one month or year for a rule, applying BYMONTHDAY, BYDAY and the invalid-date policy. */
private class MonthDays(
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
            return listOfNotNull(resolve(year, start.month, start.day))
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
}
