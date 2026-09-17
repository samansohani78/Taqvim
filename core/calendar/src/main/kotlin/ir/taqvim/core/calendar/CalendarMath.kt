/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/** Number of days in [year]. */
public fun CalendarArithmetic.yearLength(year: Int): Int = (1..monthsInYear(year)).sumOf { monthLength(year, it) }

/** 1-based position of [date] within its year. */
public fun CalendarArithmetic.dayOfYear(date: CalendarDate): Int {
    val firstDay = toJdn(date(date.year, 1, 1))
    return Math.toIntExact(toJdn(date) - firstDay) + 1
}

/** Signed number of days from [from] to [to]. */
public fun CalendarArithmetic.daysBetween(
    from: CalendarDate,
    to: CalendarDate,
): Long = toJdn(to) - toJdn(from)

/** The date [days] after [date] (negative values go back). */
public fun CalendarArithmetic.plusDays(
    date: CalendarDate,
    days: Long,
): CalendarDate = fromJdn(toJdn(date) + days)

/**
 * The date [months] after [date], keeping the day of month and clamping it to the target month's length
 * (e.g. 31 → 30). Constant time when the calendar has [CalendarArithmetic.monthsPerYear]; otherwise it steps year by
 * year, which works for any number of months per year. Throws [ArithmeticException] when the year leaves `Int`.
 */
public fun CalendarArithmetic.addMonths(
    date: CalendarDate,
    months: Int,
): CalendarDate {
    val perYear = monthsPerYear?.toLong() ?: return addMonthsYearByYear(date, months)
    val index = Math.addExact(Math.multiplyExact(date.year.toLong(), perYear), date.month - 1L + months)
    val year = Math.toIntExact(Math.floorDiv(index, perYear))
    val month = Math.floorMod(index, perYear).toInt() + 1
    return date(year, month, minOf(date.day, monthLength(year, month)))
}

private fun CalendarArithmetic.addMonthsYearByYear(
    date: CalendarDate,
    months: Int,
): CalendarDate {
    var year = date.year
    var monthIndex = date.month - 1L + months
    while (monthIndex < 0) {
        year = Math.subtractExact(year, 1)
        monthIndex += monthsInYear(year)
    }
    while (monthIndex >= monthsInYear(year)) {
        monthIndex -= monthsInYear(year)
        year = Math.addExact(year, 1)
    }
    val month = monthIndex.toInt() + 1
    return date(year, month, minOf(date.day, monthLength(year, month)))
}

/**
 * Whole months from [from] to [to]: the largest `n` (by magnitude) such that `addMonths(from, n)` does not pass
 * [to]. Negative when [to] precedes [from]. Constant time when the calendar has [CalendarArithmetic.monthsPerYear];
 * throws [ArithmeticException] when the count does not fit `Int`.
 */
public fun CalendarArithmetic.monthsBetween(
    from: CalendarDate,
    to: CalendarDate,
): Int {
    if (toJdn(to) < toJdn(from)) return Math.negateExact(monthsBetween(to, from))
    val perYear = monthsPerYear?.toLong() ?: return monthsBetweenByStepping(from, to)
    // Month numbers apart; one fewer when the day of [from] does not fit before [to] in the target month.
    val apart = (to.year.toLong() - from.year) * perYear + (to.month - from.month)
    val months = if (apart > 0 && toJdn(addMonths(from, Math.toIntExact(apart))) > toJdn(to)) apart - 1 else apart
    return Math.toIntExact(months)
}

private fun CalendarArithmetic.monthsBetweenByStepping(
    from: CalendarDate,
    to: CalendarDate,
): Int {
    var months = 0
    while (toJdn(addMonths(from, months + 1)) <= toJdn(to)) months++
    return months
}

/**
 * The [n]th [weekday] of [month] in [year] (n = 1 is the first), or `null` when the month has fewer than [n] such
 * weekdays. [n] must be positive; use [lastWeekdayOfMonth] for "last".
 */
public fun CalendarArithmetic.nthWeekdayOfMonth(
    year: Int,
    month: Int,
    weekday: Weekday,
    n: Int,
): CalendarDate? {
    require(n >= 1) { "n must be ≥ 1 (was $n)" }
    val first = toJdn(date(year, month, 1))
    val candidate = first + weekday.daysAfter(first.weekday()) + DAYS_PER_WEEK * (n - 1)
    return fromJdn(candidate).takeIf { it.year == year && it.month == month }
}

/** The last [weekday] of [month] in [year], moved by [offsetDays] (which may leave the month). */
public fun CalendarArithmetic.lastWeekdayOfMonth(
    year: Int,
    month: Int,
    weekday: Weekday,
    offsetDays: Int = 0,
): CalendarDate {
    val last = toJdn(date(year, month, monthLength(year, month)))
    return fromJdn(last - last.weekday().daysAfter(weekday) + offsetDays)
}

/** A calendar-relative difference of whole years, months and days; all components share one sign. */
public data class DatePeriod(
    public val years: Int,
    public val months: Int,
    public val days: Int,
)

/**
 * Difference from [from] to [to] as years, months and remaining days, measured in this calendar (e.g. from
 * 1404-11-30 to 1405-01-01 in the Persian calendar). Components are negative when [to] precedes [from].
 * A whole year is as many months as the year it starts in has, so the Hebrew calendar's 13-month years count as one
 * year each.
 */
public fun CalendarArithmetic.periodBetween(
    from: CalendarDate,
    to: CalendarDate,
): DatePeriod {
    if (toJdn(to) < toJdn(from)) {
        val forward = periodBetween(to, from)
        return DatePeriod(-forward.years, -forward.months, -forward.days)
    }
    val totalMonths = monthsBetween(from, to)
    val anchor = addMonths(from, totalMonths)
    val days = Math.toIntExact(daysBetween(anchor, to))
    val perYear =
        monthsPerYear ?: return wholeYearsAndMonths(from.year, totalMonths).let { (years, months) ->
            DatePeriod(years, months, days)
        }
    val years = totalMonths / perYear
    return DatePeriod(years, totalMonths - years * perYear, days)
}

/** Splits [totalMonths] (non-negative) into whole years starting at [firstYear] and the months left over. */
private fun CalendarArithmetic.wholeYearsAndMonths(
    firstYear: Int,
    totalMonths: Int,
): Pair<Int, Int> {
    var years = 0
    var left = totalMonths
    while (left >= monthsInYear(firstYear + years)) {
        left -= monthsInYear(firstYear + years)
        years++
    }
    return years to left
}

/**
 * Week numbering rule (A-08): weeks start on [firstDayOfWeek]; week 1 is the first week containing at least
 * [minimalDaysInFirstWeek] days of the year. ISO 8601 is Monday / 4; "week 1 contains the first day" is 1.
 */
public data class WeekRule(
    public val firstDayOfWeek: Weekday,
    public val minimalDaysInFirstWeek: Int,
) {
    init {
        require(minimalDaysInFirstWeek in 1..DAYS_PER_WEEK) {
            "minimal days must be in 1..7 (was $minimalDaysInFirstWeek)"
        }
    }

    public companion object {
        /** ISO 8601: Monday start, week 1 contains the first Thursday. */
        public val ISO: WeekRule = WeekRule(Weekday.MONDAY, minimalDaysInFirstWeek = 4)

        /** Week 1 is the week containing the first day of the year, starting on [firstDayOfWeek]. */
        public fun containingFirstDay(firstDayOfWeek: Weekday): WeekRule =
            WeekRule(firstDayOfWeek, minimalDaysInFirstWeek = 1)
    }
}

/** A week number and the week-based year it belongs to (which can differ from the calendar year at year edges). */
public data class WeekOfYear(
    public val weekBasedYear: Int,
    public val week: Int,
)

/** Week of [date] under [rule]. */
public fun CalendarArithmetic.weekOfYear(
    date: CalendarDate,
    rule: WeekRule,
): WeekOfYear {
    val jdn = toJdn(date)
    val ownStart = firstWeekStart(date.year, rule)
    val (weekYear, start) =
        when {
            jdn < ownStart -> (date.year - 1) to firstWeekStart(date.year - 1, rule)
            jdn >= firstWeekStart(date.year + 1, rule) -> (date.year + 1) to firstWeekStart(date.year + 1, rule)
            else -> date.year to ownStart
        }
    return WeekOfYear(weekYear, Math.toIntExact((jdn - start) / DAYS_PER_WEEK) + 1)
}

/** First day of week 1 of [year] under [rule]. */
private fun CalendarArithmetic.firstWeekStart(
    year: Int,
    rule: WeekRule,
): Jdn {
    val firstDay = toJdn(date(year, 1, 1))
    val daysSinceWeekStart = firstDay.weekday().daysAfter(rule.firstDayOfWeek)
    val weekStart = firstDay - daysSinceWeekStart
    val daysOfYearInThatWeek = DAYS_PER_WEEK - daysSinceWeekStart
    return if (daysOfYearInThatWeek >= rule.minimalDaysInFirstWeek) weekStart else weekStart + DAYS_PER_WEEK
}

/** Position of a date within a three-month season (months 1–3 are season 0, 4–6 season 1, …). */
public data class SeasonPosition(
    public val season: Int,
    public val dayOfSeason: Int,
    public val seasonLength: Int,
)

/**
 * [date]'s position within its three-month season. For the Persian calendar the seasons coincide with the
 * astronomical ones (Farvardin–Khordad = spring). Requires a 12-month year.
 */
public fun CalendarArithmetic.positionInSeason(date: CalendarDate): SeasonPosition {
    require(monthsInYear(date.year) == MONTHS_PER_YEAR) { "seasons need a 12-month year" }
    val season = (date.month - 1) / MONTHS_PER_SEASON
    val firstMonth = season * MONTHS_PER_SEASON + 1
    val dayOfSeason = (firstMonth until date.month).sumOf { monthLength(date.year, it) } + date.day
    val length = (firstMonth until firstMonth + MONTHS_PER_SEASON).sumOf { monthLength(date.year, it) }
    return SeasonPosition(season, dayOfSeason, length)
}

private const val DAYS_PER_WEEK = 7
private const val MONTHS_PER_SEASON = 3
private const val MONTHS_PER_YEAR = 12
