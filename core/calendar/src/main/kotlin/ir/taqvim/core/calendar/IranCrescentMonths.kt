/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchMoonPhase

/**
 * Month starts of the Iranian calculated-observational lunar calendar for every month, computed rather than stored
 * (ADR-0027). Months are indexed from 1 Muharram AH 1: index = (year − 1) × 12 + (month − 1).
 *
 * - AH [FIRST_ASTRONOMICAL_YEAR]‥[LAST_ASTRONOMICAL_YEAR] (and 1 Muharram of the year after): the sighting start of a
 *   month ([computeSightingStart]) is the day after the first evening, from its conjunction's civil day in Iran on, on
 *   which [IranCrescentSighting] sees the crescent; month starts follow [AnchoredMonthStarts]. Both are computed on
 *   first use, a Hijri year at a time, and kept.
 * - Every other month: the mean lunation continued from the nearer edge with the exact mean month between the two edge
 *   months ([MeanCrescentMonth]). Far outside that range the ephemeris and ΔT are only extrapolations, so no crescent is
 *   claimed there; every month still has 29 or 30 days and the continuation meets the astronomical months exactly.
 *
 * Month arithmetic is exact in [Long] for every [Int] year.
 */
internal object IranCrescentMonths {
    /** First Hijri year computed from the crescent. */
    const val FIRST_ASTRONOMICAL_YEAR: Int = -3000

    /** Last Hijri year computed from the crescent (the following 1 Muharram is computed too). */
    const val LAST_ASTRONOMICAL_YEAR: Int = 3000

    /** Months in a Hijri year. */
    const val MONTHS: Int = 12

    /** Evenings, from the conjunction day on, on which the crescent is looked for before the month starts anyway. */
    const val MAX_EVENINGS: Int = 4

    /** Meeus lunation number k (Astronomical Algorithms, ch. 49) of the conjunction before 1 Muharram AH 1. */
    private const val LUNATION_OF_FIRST_MONTH = -17_037L
    private const val YEARS_PER_CHUNK = 100
    private const val NEW_MOON_LONGITUDE = 0.0
    private const val SEARCH_BEFORE_DAYS = 3.0
    private const val SEARCH_DAYS = 6.0
    private const val TEHRAN_OFFSET_MILLIS = 12_600_000L
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val UNIX_EPOCH_JDN = 2_440_588L

    /** Index of 1 Muharram [FIRST_ASTRONOMICAL_YEAR]. */
    val FIRST_INDEX: Long = monthIndex(FIRST_ASTRONOMICAL_YEAR.toLong(), 1)

    /** Index of 1 Muharram of the year after [LAST_ASTRONOMICAL_YEAR]. */
    val LAST_INDEX: Long = monthIndex(LAST_ASTRONOMICAL_YEAR + 1L, 1)

    private val sightingYears = yearChunks { first, size -> LongArray(size) { computeSightingStart(first + it) } }

    private val startYears =
        yearChunks { first, size ->
            LongArray(size) { AnchoredMonthStarts.start(first + it, FIRST_INDEX, ::sightingStart) }
        }

    private val mean: Lazy<MeanCrescentMonth> =
        lazy { MeanCrescentMonth(crescentStart(FIRST_INDEX), crescentStart(LAST_INDEX), LAST_INDEX - FIRST_INDEX) }

    /** Month index of [month] of [year]. */
    fun monthIndex(
        year: Long,
        month: Int,
    ): Long = (year - 1) * MONTHS + (month - 1)

    /** Hijri year of the month at [index]. */
    fun yearOf(index: Long): Long = Math.floorDiv(index, MONTHS.toLong()) + 1

    /** Month number (1‥12) of the month at [index]. */
    fun monthOf(index: Long): Int = Math.floorMod(index, MONTHS.toLong()).toInt() + 1

    /** JDN of the first day of the month at [index]. */
    fun startJdn(index: Long): Long =
        when {
            index < FIRST_INDEX -> mean.value.fromFirst(index - FIRST_INDEX)
            index > LAST_INDEX -> mean.value.fromLast(index - LAST_INDEX)
            else -> crescentStart(index)
        }

    /** A month index within a month or two of the one containing [jdn]. */
    fun estimateIndex(jdn: Long): Long = mean.value.estimateIndex(jdn, LAST_INDEX)

    /** The sighting start of the month at [index] (in [FIRST_INDEX]‥[LAST_INDEX]), computed once and kept. */
    fun sightingStart(index: Long): Long = lookUp(sightingYears, index)

    /**
     * JDN of the day after the first evening, from the civil day in Iran (UTC+03:30) of the conjunction that begins the
     * month at [index] on, on which the crescent is seen; after the last of [MAX_EVENINGS] evenings if it is not seen.
     */
    fun computeSightingStart(index: Long): Long {
        val lunation = index + LUNATION_OF_FIRST_MONTH
        val searchFrom = Time(MeanNewMoon.ut(lunation) - SEARCH_BEFORE_DAYS)
        val conjunction =
            checkNotNull(searchMoonPhase(NEW_MOON_LONGITUDE, searchFrom, SEARCH_DAYS)) {
                "The ephemeris found no new moon for lunation $lunation"
            }
        val civilDay =
            Math.floorDiv(conjunction.toMillisecondsSince1970() + TEHRAN_OFFSET_MILLIS, MILLIS_PER_DAY) + UNIX_EPOCH_JDN
        val evening = (civilDay until civilDay + MAX_EVENINGS).firstOrNull(IranCrescentSighting::seenOnEvening)
        return (evening ?: (civilDay + MAX_EVENINGS - 1)) + 1
    }

    private fun crescentStart(index: Long): Long = lookUp(startYears, index)

    private fun lookUp(
        years: List<Lazy<List<Lazy<LongArray>>>>,
        index: Long,
    ): Long {
        val offset = index - FIRST_INDEX
        val year = Math.floorDiv(offset, MONTHS.toLong()).toInt()
        val month = Math.floorMod(offset, MONTHS.toLong()).toInt()
        return years[year / YEARS_PER_CHUNK].value[year % YEARS_PER_CHUNK].value[month]
    }

    /** Lazily built values per Hijri year of the astronomical range, [YEARS_PER_CHUNK] years per chunk. */
    private fun yearChunks(year: (first: Long, size: Int) -> LongArray): List<Lazy<List<Lazy<LongArray>>>> =
        List((LAST_ASTRONOMICAL_YEAR + 1 - FIRST_ASTRONOMICAL_YEAR) / YEARS_PER_CHUNK + 1) { chunk ->
            lazy {
                List(YEARS_PER_CHUNK) { offset ->
                    lazy {
                        val first = FIRST_INDEX + (chunk * YEARS_PER_CHUNK + offset).toLong() * MONTHS
                        year(first, if (first == LAST_INDEX) 1 else MONTHS)
                    }
                }
            }
        }
}

/**
 * Month starts from sighting starts (ADR-0027). A lunar month has 29 or 30 days, so a month start is the sighting start
 * kept within 29 and 30 days of the previous month start: when the crescent is first seen only on the evening of day 30
 * the month is completed to 30 days, and a crescent seen on the evening of day 28 is not taken. The chain of months
 * restarts on the sighting start at every anchor month, one whose two previous sighting months have 30 and 29 days in
 * either order; that pattern brings any start that was a day early or late back onto the sighting start, so every
 * month is computed on its own, from the nearest anchor.
 */
internal object AnchoredMonthStarts {
    private const val SHORT_MONTH = 29L
    private const val LONG_MONTH = 30L

    /** Month start at [index], with sighting starts from [sighting] defined from [firstIndex] on. */
    fun start(
        index: Long,
        firstIndex: Long,
        sighting: (Long) -> Long,
    ): Long {
        var anchor = index
        while (anchor > firstIndex && !(anchor >= firstIndex + 2 && isAnchor(anchor, sighting))) anchor--
        var start = sighting(anchor)
        for (next in anchor + 1..index) start = sighting(next).coerceIn(start + SHORT_MONTH, start + LONG_MONTH)
        return start
    }

    /** Whether the two sighting months before [index] have 30 and 29 days, in either order. */
    fun isAnchor(
        index: Long,
        sighting: (Long) -> Long,
    ): Boolean {
        val earlier = sighting(index - 1) - sighting(index - 2)
        val later = sighting(index) - sighting(index - 1)
        return (earlier == LONG_MONTH && later == SHORT_MONTH) || (earlier == SHORT_MONTH && later == LONG_MONTH)
    }
}

/** Jean Meeus, Astronomical Algorithms 2nd ed., eq. (49.1): the mean new moon of lunation k, as days from J2000. */
internal object MeanNewMoon {
    private const val LUNATIONS_PER_CENTURY = 1_236.85
    private const val J2000_JULIAN_DAY = 2_451_545.0

    @Suppress("MagicNumber") // Coefficients of Meeus eq. (49.1).
    fun ut(lunation: Long): Double {
        val k = lunation.toDouble()
        val t = k / LUNATIONS_PER_CENTURY
        val julianEphemerisDay =
            2_451_550.097_66 + 29.530_588_861 * k + 0.000_154_37 * t * t - 0.000_000_150 * t * t * t +
                0.000_000_000_73 * t * t * t * t
        return julianEphemerisDay - J2000_JULIAN_DAY
    }
}

/**
 * The mean lunation continued from the edge months starting at [firstStart] and [lastStart], [months] months apart.
 * The mean month (lastStart − firstStart) / months is used exactly through integer division, so every continued month
 * has 29 or 30 days and the months at the edges are met exactly.
 */
internal class MeanCrescentMonth(
    private val firstStart: Long,
    private val lastStart: Long,
    private val months: Long,
) {
    private val days = lastStart - firstStart

    /** JDN of the month [offset] months after the first edge month (negative values go back). */
    fun fromFirst(offset: Long): Long = firstStart + Math.floorDiv(Math.multiplyExact(offset, days), months)

    /** JDN of the month [offset] months after the last edge month. */
    fun fromLast(offset: Long): Long = lastStart + Math.floorDiv(Math.multiplyExact(offset, days), months)

    /** A month index near the one containing [jdn], counted in mean months from [lastIndex]. */
    fun estimateIndex(
        jdn: Long,
        lastIndex: Long,
    ): Long = lastIndex + Math.floorDiv(Math.multiplyExact(jdn - lastStart, months), days)
}
