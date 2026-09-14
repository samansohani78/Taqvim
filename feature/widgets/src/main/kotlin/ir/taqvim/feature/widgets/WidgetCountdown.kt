/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.DatePeriod
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.calendar.periodBetween
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.math.abs

/** Whether a countdown counts the days until its date or the time since it (F-09). */
enum class CountdownMode {
    UNTIL,
    SINCE,
}

/**
 * The date a countdown widget counts to or from (T-1212, F-09): [year]-[month]-[day] in [calendar]. [repeatsYearly]
 * counts to the next anniversary (e.g. a birthday or Nowruz); [startJdn] is the day the date was chosen, from which a
 * one-off countdown's progress is measured. A blank [title] shows the date instead.
 */
data class WidgetCountdown(
    val calendar: CalendarSystem,
    val year: Int,
    val month: Int,
    val day: Int,
    val startJdn: Long,
    val mode: CountdownMode = CountdownMode.UNTIL,
    val repeatsYearly: Boolean = false,
    val title: String = "",
) {
    /** This countdown with a title of at most [MAX_TITLE_LENGTH] characters and a month and day of at least 1. */
    fun normalized(): WidgetCountdown =
        copy(title = title.take(MAX_TITLE_LENGTH), month = month.coerceAtLeast(1), day = day.coerceAtLeast(1))

    companion object {
        const val MAX_TITLE_LENGTH: Int = 40
    }
}

/** Where a countdown stands on a day. */
enum class CountdownStatus {
    /** The date (or its next anniversary) is still ahead. */
    UPCOMING,

    /** The date, or one of its anniversaries, is today. */
    TODAY,

    /** A one-off date has passed. */
    PASSED,

    /** Time since the date, which has passed. */
    ELAPSED,

    /** Time since a date that has not come yet. */
    NOT_YET,
}

/**
 * A countdown evaluated on a day: its [status], the [target] day (the date or its next anniversary), the whole [days]
 * between the target and that day (never negative), the calendar [period] since the date in [CountdownMode.SINCE]
 * once it has come, and the [progress] 0 … 1 of the current stretch.
 */
data class CountdownResult(
    val status: CountdownStatus,
    val target: Jdn,
    val days: Long,
    val period: DatePeriod?,
    val progress: Float,
)

/**
 * Countdown arithmetic in the countdown's own calendar (F-09). An anniversary keeps the month and day and clamps the
 * day to the month's length, so 30 Esfand falls on 29 Esfand and 29 February on 28 February in common years.
 */
object WidgetCountdownMath {
    private const val MONTHS_PER_YEAR = 12

    /** The countdown's date in [calendar], with the month and day clamped to ones that exist in its year. */
    fun origin(
        countdown: WidgetCountdown,
        calendar: CalendarArithmetic,
    ): CalendarDate {
        val month = countdown.month.coerceIn(1, calendar.monthsInYear(countdown.year))
        val day = countdown.day.coerceIn(1, calendar.monthLength(countdown.year, month))
        return calendar.date(countdown.year, month, day)
    }

    /** The anniversary of [origin] in [year] of [calendar]. */
    fun anniversary(
        origin: CalendarDate,
        year: Int,
        calendar: CalendarArithmetic,
    ): CalendarDate = calendar.addMonths(origin, MONTHS_PER_YEAR * (year - origin.year))

    /** [countdown] on [today], computed in [calendar] (the arithmetic of the countdown's calendar). */
    fun evaluate(
        countdown: WidgetCountdown,
        calendar: CalendarArithmetic,
        today: Jdn,
    ): CountdownResult {
        val origin = origin(countdown, calendar)
        return when {
            countdown.mode == CountdownMode.SINCE -> since(origin, calendar, today)
            countdown.repeatsYearly -> nextAnniversary(origin, calendar, today)
            else -> oneOff(calendar.toJdn(origin), Jdn(countdown.startJdn), today)
        }
    }

    private fun since(
        origin: CalendarDate,
        calendar: CalendarArithmetic,
        today: Jdn,
    ): CountdownResult {
        val originJdn = calendar.toJdn(origin)
        if (originJdn > today) return CountdownResult(CountdownStatus.NOT_YET, originJdn, originJdn - today, null, 0f)
        val year = calendar.fromJdn(today).year
        val last = if (calendar.toJdn(anniversary(origin, year, calendar)) <= today) year else year - 1
        val from = calendar.toJdn(anniversary(origin, last, calendar))
        val to = calendar.toJdn(anniversary(origin, last + 1, calendar))
        return CountdownResult(
            status = if (originJdn == today) CountdownStatus.TODAY else CountdownStatus.ELAPSED,
            target = originJdn,
            days = today - originJdn,
            period = calendar.periodBetween(origin, calendar.fromJdn(today)),
            progress = fraction(today - from, to - from),
        )
    }

    private fun nextAnniversary(
        origin: CalendarDate,
        calendar: CalendarArithmetic,
        today: Jdn,
    ): CountdownResult {
        val year = calendar.fromJdn(today).year
        val next = if (calendar.toJdn(anniversary(origin, year, calendar)) >= today) year else year + 1
        val target = calendar.toJdn(anniversary(origin, next, calendar))
        val previous = calendar.toJdn(anniversary(origin, next - 1, calendar))
        val days = target - today
        val status = if (days == 0L) CountdownStatus.TODAY else CountdownStatus.UPCOMING
        return CountdownResult(status, target, days, null, fraction(today - previous, target - previous))
    }

    private fun oneOff(
        target: Jdn,
        start: Jdn,
        today: Jdn,
    ): CountdownResult {
        val days = target - today
        val status =
            when {
                days > 0 -> CountdownStatus.UPCOMING
                days == 0L -> CountdownStatus.TODAY
                else -> CountdownStatus.PASSED
            }
        val progress = if (days <= 0) 1f else fraction(today - start, target - start)
        return CountdownResult(status, target, abs(days), null, progress)
    }

    private fun fraction(
        done: Long,
        total: Long,
    ): Float = if (total <= 0) 1f else (done.toFloat() / total).coerceIn(0f, 1f)
}
