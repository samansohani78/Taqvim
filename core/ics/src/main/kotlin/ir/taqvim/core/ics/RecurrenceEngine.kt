/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.addMonths
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
 *
 * Per frequency, as in the RFC's table: [byDay] and [byMonthDay] limit the candidate day of a DAILY rule (an ordinal
 * in [byDay] is ignored there, since the RFC forbids one); [byDay] expands a WEEKLY rule and [byMonthDay] does not
 * apply to it; both expand a MONTHLY or YEARLY rule and intersect when given together.
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
 * consecutive periods ends its sequence instead of searching forever, and a sequence ends before its periods leave the
 * days a date can hold ([CalendarLimits.LAST_DAY]), so huge intervals never overflow.
 *
 * Each period is computed directly from its index (months with [CalendarArithmetic.monthsPerYear]), so reaching a
 * period costs the same whatever its distance from the start; [occurrences] with a `from` day also skips the earlier
 * periods entirely when the rule has no COUNT (review I03).
 */
public class RecurrenceEngine(
    private val calendar: CalendarArithmetic,
) {
    /** Occurrences (as day numbers) of [rule] starting at [start], lazily and in ascending order. */
    public fun occurrences(
        start: CalendarDate,
        rule: RecurrenceRule,
    ): Sequence<Jdn> = occurrences(start, rule, from = null)

    /**
     * The occurrences of [rule] starting at [start] that fall on or after [from] (all of them when `null`), lazily
     * and in ascending order. Without COUNT the expansion begins at the period holding [from]; with COUNT, which is
     * counted from [start], the earlier occurrences are still generated (at most COUNT of them) and dropped.
     */
    public fun occurrences(
        start: CalendarDate,
        rule: RecurrenceRule,
        from: Jdn?,
    ): Sequence<Jdn> {
        require(start.system == calendar.system) { "start must be a ${calendar.system} date (was ${start.system})" }
        val first = calendar.toJdn(start)
        val firstIndex = if (from == null || rule.count != null) 0L else seekIndex(start, rule, from)
        val later = periods(start, rule, firstIndex).map(::Jdn).filter { it > first }
        val all = sequenceOf(first) + later
        val bounded = rule.until?.let { until -> all.takeWhile { it <= until } } ?: all
        val counted = rule.count?.let { bounded.take(it) } ?: bounded
        return if (from == null) counted else counted.filter { it >= from }
    }

    /**
     * The index of the period before the one holding [from] (a day that [InvalidDatePolicy.NEXT_DAY] pushes past its
     * own period can still fall on or after [from]); `0` when [from] is not after [start].
     */
    private fun seekIndex(
        start: CalendarDate,
        rule: RecurrenceRule,
        from: Jdn,
    ): Long {
        val first = calendar.toJdn(start).value
        if (from.value <= first) return 0
        val interval = rule.interval.toLong()
        val periods =
            when (rule.frequency) {
                Frequency.DAILY -> (from.value - first) / interval
                Frequency.WEEKLY -> (from.value - weekBegins(start, rule, 0)) / (DAYS_PER_WEEK * interval)
                Frequency.MONTHLY -> monthsApart(start, calendar.fromJdn(from)) / interval
                Frequency.YEARLY -> (calendar.fromJdn(from).year.toLong() - start.year) / interval
            }
        return maxOf(0L, periods - 1)
    }

    /** Month numbers from the month of [from] to the month of [to] (not before it). */
    private fun monthsApart(
        from: CalendarDate,
        to: CalendarDate,
    ): Long {
        val perYear = calendar.monthsPerYear?.toLong()
        if (perYear != null) return (to.year.toLong() - from.year) * perYear + (to.month - from.month)
        var months = 0L
        var year = from.year
        while (year < to.year) {
            months += calendar.monthsInYear(year)
            year++
        }
        return months + to.month - from.month
    }

    /** Candidate days of periods [firstIndex] onwards, merged so days pushed into the next period stay in order. */
    private fun periods(
        start: CalendarDate,
        rule: RecurrenceRule,
        firstIndex: Long,
    ): Sequence<Long> =
        sequence {
            val pending = TreeSet<Long>()
            var emptyPeriods = 0
            var index = firstIndex
            while (emptyPeriods < MAX_EMPTY_PERIODS) {
                val (periodStart, days) = period(start, rule, index) ?: break
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

    /** The calendar year holding [CalendarLimits.LAST_DAY], computed only for periods that may reach it. */
    private val lastYear: Int by lazy { calendar.fromJdn(CalendarLimits.LAST_DAY).year }

    /** First day of the [index]-th period and the candidate days in it, or `null` once periods pass the last day. */
    private fun period(
        start: CalendarDate,
        rule: RecurrenceRule,
        index: Long,
    ): Pair<Long, List<Long>>? {
        val steps = Math.multiplyExact(index, rule.interval.toLong())
        val first = calendar.toJdn(start).value
        return when (rule.frequency) {
            Frequency.DAILY -> {
                val day = first + steps
                if (day > CalendarLimits.LAST_DAY.value) null else day to dailyCandidate(rule, day)
            }

            Frequency.WEEKLY -> {
                val weekBegins = weekBegins(start, rule, steps)
                if (weekBegins > CalendarLimits.LAST_DAY.value) null else weekly(start, rule, weekBegins)
            }

            Frequency.MONTHLY -> {
                monthly(start, rule, steps)
            }

            Frequency.YEARLY -> {
                yearly(start, rule, steps)
            }
        }
    }

    private fun monthly(
        start: CalendarDate,
        rule: RecurrenceRule,
        steps: Long,
    ): Pair<Long, List<Long>>? {
        val (year, month) = monthAfter(start, steps) ?: return null
        val first = calendar.toJdn(start).value
        if (mayPassLimit(first, steps, MAX_MONTH_DAYS) && beyondLimit(year, month)) return null
        return firstDay(year.toInt(), month) to MonthDays(calendar, rule, start).inMonth(year.toInt(), month)
    }

    private fun yearly(
        start: CalendarDate,
        rule: RecurrenceRule,
        steps: Long,
    ): Pair<Long, List<Long>>? {
        val year = start.year + steps
        val first = calendar.toJdn(start).value
        if (mayPassLimit(first, steps, MAX_YEAR_DAYS) && beyondLimit(year, 1)) return null
        return firstDay(year.toInt(), 1) to MonthDays(calendar, rule, start).inYear(year.toInt())
    }

    /** Whether a period [steps] months or years (≤ [maxDays] days each) after [first] may begin after the last day. */
    private fun mayPassLimit(
        first: Long,
        steps: Long,
        maxDays: Long,
    ): Boolean = steps > (CalendarLimits.LAST_DAY.value - first) / maxDays

    /** Whether [month] of [year] begins after [CalendarLimits.LAST_DAY]. */
    private fun beyondLimit(
        year: Long,
        month: Int,
    ): Boolean =
        when {
            year < lastYear -> false
            year > lastYear -> true
            else -> firstDay(year.toInt(), month) > CalendarLimits.LAST_DAY.value
        }

    /** [day] itself when BYDAY and BYMONTHDAY both accept it (they limit a DAILY rule), else no candidate. */
    private fun dailyCandidate(
        rule: RecurrenceRule,
        day: Long,
    ): List<Long> {
        val byDay = rule.byDay.isEmpty() || rule.byDay.any { it.weekday == Jdn(day).weekday() }
        val byMonthDay = rule.byMonthDay.isEmpty() || rule.byMonthDay.any { monthDayMatches(day, it) }
        return if (byDay && byMonthDay) listOf(day) else emptyList()
    }

    /** Whether [day] is the [monthDay]-th day of its month, counted from the month's end when negative. */
    private fun monthDayMatches(
        day: Long,
        monthDay: Int,
    ): Boolean {
        val date = calendar.fromJdn(Jdn(day))
        val target = if (monthDay > 0) monthDay else calendar.monthLength(date.year, date.month) + monthDay + 1
        return date.day == target
    }

    /** First day of the week [steps] weeks after the week holding [start]. */
    private fun weekBegins(
        start: CalendarDate,
        rule: RecurrenceRule,
        steps: Long,
    ): Long {
        val first = calendar.toJdn(start)
        return first.value - first.weekday().daysAfter(rule.weekStart) + steps * DAYS_PER_WEEK
    }

    private fun weekly(
        start: CalendarDate,
        rule: RecurrenceRule,
        weekBegins: Long,
    ): Pair<Long, List<Long>> {
        val weekdays = rule.byDay.map { it.weekday }.ifEmpty { listOf(calendar.toJdn(start).weekday()) }
        return weekBegins to weekdays.map { weekBegins + it.daysAfter(rule.weekStart) }.distinct().sorted()
    }

    private fun firstDay(
        year: Int,
        month: Int,
    ): Long = calendar.toJdn(calendar.date(year, month, 1)).value

    /**
     * Year and month [steps] months after the month of [start], directly when the calendar has a fixed month count
     * (else year by year, and `null` beyond `Int` months).
     */
    private fun monthAfter(
        start: CalendarDate,
        steps: Long,
    ): Pair<Long, Int>? {
        val perYear = calendar.monthsPerYear?.toLong()
        if (perYear == null) {
            if (steps > Int.MAX_VALUE) return null
            val moved = calendar.addMonths(calendar.date(start.year, start.month, 1), steps.toInt())
            return moved.year.toLong() to moved.month
        }
        val monthIndex = start.year * perYear + (start.month - 1) + steps
        return Math.floorDiv(monthIndex, perYear) to (Math.floorMod(monthIndex, perYear).toInt() + 1)
    }

    private companion object {
        const val MAX_EMPTY_PERIODS = 1_000
        const val DAYS_PER_WEEK = 7L

        // Upper bounds for the limit check: no calendar month is longer than 32 days and no year longer than 390.
        const val MAX_MONTH_DAYS = 32L
        const val MAX_YEAR_DAYS = 390L
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
