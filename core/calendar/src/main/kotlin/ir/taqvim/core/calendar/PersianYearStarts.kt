/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.floor
import kotlin.time.Instant

private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_DAY = 86_400_000L
private const val NOON_MILLIS = MILLIS_PER_DAY / 2
private const val UNIX_EPOCH_JDN = 2_440_588L
private val TEHRAN_OFFSET_MILLIS: Long = PersianYearStartRule.TEHRAN_MERIDIAN_OFFSET.totalSeconds * MILLIS_PER_SECOND

/**
 * 1 Farvardin of every Persian year by the A-02 rule of [PersianYearStartRule], computed rather than stored (ADR-0026).
 *
 * - [FIRST_ASTRONOMICAL_YEAR]‥[LAST_ASTRONOMICAL_YEAR]: the rule applied to the true March equinox from
 *   [CalendarAstronomy]. Years are computed on first use, [BLOCK_YEARS] at a time, and kept.
 * - Every other year: the same rule applied to the mean March equinox, continued from the nearer edge with the mean
 *   interval between the two edge equinoxes ([MeanMarchEquinox]). Outside the range the ephemeris and ΔT are only
 *   extrapolations, so no true equinox is claimed there; every year still has 365 or 366 days, and at each edge the
 *   continuation gives exactly the astronomical start.
 *
 * Day arithmetic is exact in [Long] for every [Int] year and the year after [Int.MAX_VALUE].
 */
internal object PersianYearStarts {
    /** First year computed from the true March equinox. */
    const val FIRST_ASTRONOMICAL_YEAR: Int = -3000

    /** Last year computed from the true March equinox. */
    const val LAST_ASTRONOMICAL_YEAR: Int = 3000

    private const val ASTRONOMICAL_SPAN = LAST_ASTRONOMICAL_YEAR - FIRST_ASTRONOMICAL_YEAR
    private const val BLOCK_YEARS = 64

    /** The March equinox that starts Persian year y falls in Gregorian year y + 621. */
    private const val GREGORIAN_YEAR_OFFSET = 621

    private val blocks: List<Lazy<LongArray>> = List(ASTRONOMICAL_SPAN / BLOCK_YEARS + 1) { lazy { block(it) } }

    private val mean: Lazy<MeanMarchEquinox> =
        lazy { MeanMarchEquinox(equinox(FIRST_ASTRONOMICAL_YEAR), equinox(LAST_ASTRONOMICAL_YEAR), ASTRONOMICAL_SPAN) }

    /** JDN of 1 Farvardin [year]. */
    fun startJdn(year: Long): Long =
        when {
            year < FIRST_ASTRONOMICAL_YEAR -> mean.value.startJdnFromFirst(year - FIRST_ASTRONOMICAL_YEAR)
            year > LAST_ASTRONOMICAL_YEAR -> mean.value.startJdnFromLast(year - LAST_ASTRONOMICAL_YEAR)
            else -> astronomicalStartJdn((year - FIRST_ASTRONOMICAL_YEAR).toInt())
        }

    private fun astronomicalStartJdn(index: Int): Long = blocks[index / BLOCK_YEARS].value[index % BLOCK_YEARS]

    /** The year containing [jdn]; throws [IllegalArgumentException] for days outside the years of [Int]. */
    fun yearContaining(jdn: Long): Int {
        requireInCalendarRange(jdn >= startJdn(Int.MIN_VALUE.toLong()) && jdn < startJdn(Int.MAX_VALUE + 1L)) {
            "JDN $jdn is outside the Persian years ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
        }
        var year = mean.value.estimateYear(jdn, LAST_ASTRONOMICAL_YEAR)
        while (startJdn(year) > jdn) year--
        while (startJdn(year + 1) <= jdn) year++
        return year.toInt()
    }

    private fun block(index: Int): LongArray {
        val first = FIRST_ASTRONOMICAL_YEAR + index * BLOCK_YEARS
        return LongArray(minOf(BLOCK_YEARS, LAST_ASTRONOMICAL_YEAR - first + 1)) {
            PersianYearStartRule.firstDayOfYear(equinox(first + it)).value
        }
    }

    private fun equinox(year: Int): Instant =
        checkNotNull(CalendarAstronomy.marchEquinox(year + GREGORIAN_YEAR_OFFSET)) {
            "The ephemeris found no March equinox for Persian year $year"
        }
}

/**
 * The mean March equinox continued from the edge equinoxes [first] and [last], [span] years apart, and the A-02
 * year start it gives. The mean interval (last − first) / span is kept exactly, as whole days, milliseconds and a
 * remainder in 1/[span] ms, so year counts across the whole [Int] range need neither rounding nor more than [Long].
 */
internal class MeanMarchEquinox(
    first: Instant,
    last: Instant,
    private val span: Int,
) {
    private val firstLocalMillis = first.toEpochMilliseconds() + TEHRAN_OFFSET_MILLIS
    private val lastLocalMillis = last.toEpochMilliseconds() + TEHRAN_OFFSET_MILLIS
    private val intervalMillis = last.toEpochMilliseconds() - first.toEpochMilliseconds()
    private val wholeMillis = Math.floorDiv(intervalMillis, span.toLong())
    private val remainder = Math.floorMod(intervalMillis, span.toLong())
    private val wholeDays = Math.floorDiv(wholeMillis, MILLIS_PER_DAY)
    private val extraMillis = Math.floorMod(wholeMillis, MILLIS_PER_DAY)
    private val meanYearDays = intervalMillis.toDouble() / span / MILLIS_PER_DAY

    /** JDN of 1 Farvardin [years] years after the first edge year (negative values go back). */
    fun startJdnFromFirst(years: Long): Long = startJdn(firstLocalMillis, years)

    /** JDN of 1 Farvardin [years] years after the last edge year. */
    fun startJdnFromLast(years: Long): Long = startJdn(lastLocalMillis, years)

    private fun startJdn(
        anchorLocalMillis: Long,
        years: Long,
    ): Long {
        val local = anchorLocalMillis + years * extraMillis + Math.floorDiv(years * remainder, span.toLong())
        val day = UNIX_EPOCH_JDN + years * wholeDays + Math.floorDiv(local, MILLIS_PER_DAY)
        return if (Math.floorMod(local, MILLIS_PER_DAY) < NOON_MILLIS) day else day + 1
    }

    /** A year within a year or two of the one containing [jdn], counted in mean years from [lastYear]'s equinox. */
    fun estimateYear(
        jdn: Long,
        lastYear: Int,
    ): Long {
        val daysSinceLastEquinox = jdn - UNIX_EPOCH_JDN - lastLocalMillis.toDouble() / MILLIS_PER_DAY
        return lastYear + floor(daysSinceLastEquinox / meanYearDays).toLong()
    }
}
