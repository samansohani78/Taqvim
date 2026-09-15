/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import kotlinx.datetime.LocalDate

/**
 * The days, years and month distances screens can offer. Every Taqvim calendar computes any `Int` year from its rule
 * (no year tables), so the limits come from the types the app passes days through, not from data:
 * - days: those kotlinx.datetime can hold as a [LocalDate] (ISO years −999 999 999‥999 999 999; see [toLocalDate]);
 * - month paging: offsets and page indices counted in `Int`, as Compose pagers and widget state count them.
 */
public object CalendarLimits {
    private const val MIN_ISO_YEAR = -999_999_999
    private const val MAX_ISO_YEAR = 999_999_999
    private const val DECEMBER = 12
    private const val LAST_DAY_OF_DECEMBER = 31

    /** The first day a [LocalDate] can hold. */
    public val FIRST_DAY: Jdn = LocalDate(MIN_ISO_YEAR, 1, 1).toJdn()

    /** The last day a [LocalDate] can hold. */
    public val LAST_DAY: Jdn = LocalDate(MAX_ISO_YEAR, DECEMBER, LAST_DAY_OF_DECEMBER).toJdn()

    /**
     * Months a pager or widget moves at most from its reference month either way: half of [Int.MAX_VALUE], so an offset
     * and a page index centred on the reference month both fit in `Int`. That is about 89 million years, well inside
     * [FIRST_DAY]‥[LAST_DAY] for every calendar.
     */
    public const val MAX_MONTH_OFFSET: Int = Int.MAX_VALUE / 2

    /** Every day between [FIRST_DAY] and [LAST_DAY]. */
    public val days: JdnRange
        get() = FIRST_DAY..LAST_DAY

    /**
     * The whole years of [calendar] whose every day lies between [firstDay] and [lastDay] (by default every day a screen
     * can show); a screen with narrower days (the Astronomy screen) passes its own.
     */
    public fun years(
        calendar: CalendarArithmetic,
        firstDay: Jdn = FIRST_DAY,
        lastDay: Jdn = LAST_DAY,
    ): IntRange {
        val first = calendar.fromJdn(firstDay).year
        val last = calendar.fromJdn(lastDay).year
        val firstWhole = if (calendar.fromJdn(firstDay - 1).year != first) first else first + 1
        val lastWhole = if (calendar.fromJdn(lastDay + 1).year != last) last else last - 1
        return firstWhole..lastWhole
    }

    /** [offset] limited to ±[MAX_MONTH_OFFSET]. */
    public fun clampMonthOffset(offset: Int): Int = offset.coerceIn(-MAX_MONTH_OFFSET, MAX_MONTH_OFFSET)

    /**
     * The [years] of [calendar] a month pager around the month of [today] reaches: whole years within
     * [MAX_MONTH_OFFSET] months of it. Calendars without a fixed number of months per year count 13 months a year.
     */
    public fun pagedYears(
        calendar: CalendarArithmetic,
        today: Jdn,
    ): IntRange {
        val all = years(calendar)
        val todayYear = calendar.fromJdn(today).year
        val reach = MAX_MONTH_OFFSET / (calendar.monthsPerYear ?: MAX_MONTHS_IN_A_YEAR) - 1
        return maxOf(all.first, todayYear - reach)..minOf(all.last, todayYear + reach)
    }

    private const val MAX_MONTHS_IN_A_YEAR = 13
}
