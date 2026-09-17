/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.floor

/**
 * First days of Bikram Sambat months (A-07, ADR-0030), computed from the Surya Siddhanta sankrantis.
 *
 * Month m of year y begins with the Sun's entry into sign m − 1 ([SuryaSiddhantaSun.sankranti] index
 * 12 × (y + [KALI_YEAR_OFFSET]) + m − 1). The month's first day is the Nepal Standard Time civil day of that instant,
 * with the two night rules the national panchang follows: a Makara sankranti (Magh) after sunset starts the month the
 * next day, and a Karka sankranti (Shrawan) before sunrise starts it the previous day.
 *
 * Starts are computed on first use and kept, [BLOCK_YEARS] years at a time, for [FIRST_CACHED_YEAR]‥[LAST_CACHED_YEAR];
 * other years are computed on each call. Every [Int] year (and the year after [Int.MAX_VALUE]) is supported.
 */
internal object NepaliMonthStarts {
    /** Bikram Sambat year y begins in Kali year y + 3044 (BS 2083 = Kali 5127). */
    const val KALI_YEAR_OFFSET: Long = 3_044L

    /** JDN of the civil day that begins at the Kali epoch (midnight at Ujjain, 17/18 February 3102 BC). */
    const val KALI_EPOCH_JDN: Long = 588_466L

    const val FIRST_CACHED_YEAR: Int = -3_000
    const val LAST_CACHED_YEAR: Int = 3_000

    private const val MONTHS = 12
    private const val MAGH = 10
    private const val SHRAWAN = 4
    private const val BLOCK_YEARS = 32
    private const val CACHED_SPAN = LAST_CACHED_YEAR - FIRST_CACHED_YEAR

    /**
     * Fraction of a day by which Nepal midnight (UTC+05:45) follows Ujjain midnight (75°46′ E), so the Nepal day of a
     * Kali instant is floor(day + fraction + this).
     */
    const val NEPAL_AFTER_UJJAIN: Double = 5.75 / 24.0 - (75.0 + 46.0 / 60.0) / 360.0

    private val blocks: List<Lazy<LongArray>> = List(CACHED_SPAN / BLOCK_YEARS + 1) { lazy { block(it) } }

    /** JDN of the first day of [month] (1‥12) of [year]. */
    fun startJdn(
        year: Long,
        month: Int,
    ): Long {
        require(month in 1..MONTHS) { "Nepali month must be in 1..12 (was $month)" }
        return if (year in FIRST_CACHED_YEAR..LAST_CACHED_YEAR) {
            val index = (year - FIRST_CACHED_YEAR).toInt()
            blocks[index / BLOCK_YEARS].value[(index % BLOCK_YEARS) * MONTHS + month - 1]
        } else {
            computeStart(year, month)
        }
    }

    /** The sankranti that starts [month] of [year], as its Nepal civil day (JDN) and the fraction of that day elapsed. */
    fun sankranti(
        year: Long,
        month: Int,
    ): NepalInstant {
        val kali = SuryaSiddhantaSun.sankranti(sankrantiIndex(year, month))
        val local = kali.fraction + NEPAL_AFTER_UJJAIN
        val whole = floor(local).toLong()
        return NepalInstant(KALI_EPOCH_JDN + kali.day + whole, local - whole)
    }

    private fun computeStart(
        year: Long,
        month: Int,
    ): Long {
        val instant = sankranti(year, month)
        return when {
            month == MAGH && instant.fraction >= KathmanduDaylight.sunset(instant.jdn) -> instant.jdn + 1
            month == SHRAWAN && instant.fraction < KathmanduDaylight.sunrise(instant.jdn) -> instant.jdn - 1
            else -> instant.jdn
        }
    }

    private fun sankrantiIndex(
        year: Long,
        month: Int,
    ): Long = Math.addExact(Math.multiplyExact(Math.addExact(year, KALI_YEAR_OFFSET), MONTHS.toLong()), month - 1L)

    private fun block(index: Int): LongArray {
        val first = FIRST_CACHED_YEAR + index * BLOCK_YEARS
        val years = minOf(BLOCK_YEARS, LAST_CACHED_YEAR - first + 1)
        return LongArray(years * MONTHS) { computeStart(first + it / MONTHS.toLong(), it % MONTHS + 1) }
    }
}

/** An instant in Nepal Standard Time: civil day [jdn] and the [fraction] of it elapsed. */
internal data class NepalInstant(
    val jdn: Long,
    val fraction: Double,
)
