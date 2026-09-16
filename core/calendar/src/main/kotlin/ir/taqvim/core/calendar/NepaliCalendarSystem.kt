/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.math.floor

/**
 * Bikram Sambat, the official calendar of Nepal (A-07): twelve solar months of 29 to 32 days, each starting with a
 * sankranti of the Surya Siddhanta Sun, the solar theory of Nepal's national panchang. See [NepaliMonthStarts] and
 * docs/adr/0030-bikram-sambat-computed.md. Every [Int] year is computed; nothing is tabulated.
 */
public object NepaliCalendarSystem : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.NEPALI

    private const val MONTHS = 12
    private const val COMMON_YEAR_DAYS = 365

    /** Mean sidereal year of the Surya Siddhanta in days, for the first guess of a day's year. */
    private val MEAN_YEAR_DAYS =
        SuryaSiddhantaSun.CIVIL_DAYS_IN_MAHAYUGA.toDouble() / SuryaSiddhantaSun.SUN_REVOLUTIONS_IN_MAHAYUGA

    /** First day of [Int.MIN_VALUE] and first day after [Int.MAX_VALUE]: the days [fromJdn] accepts. */
    private val supportedDays: Lazy<LongRange> =
        lazy { startOfYear(Int.MIN_VALUE.toLong()) until startOfYear(Int.MAX_VALUE + 1L) }

    override fun isLeapYear(year: Int): Boolean = startOfYear(year + 1L) - startOfYear(year.toLong()) > COMMON_YEAR_DAYS

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Nepali month must be in 1..12 (was $month)" }
        return (nextMonthStart(year.toLong(), month) - NepaliMonthStarts.startJdn(year.toLong(), month)).toInt()
    }

    /** JDN of 1 Baisakh [year]. */
    public fun firstDayOfYear(year: Int): Jdn = Jdn(startOfYear(year.toLong()))

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected a NEPALI date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) { "Nepali date ${date.toIsoLikeString()} does not exist" }
        return Jdn(NepaliMonthStarts.startJdn(date.year.toLong(), date.month) + date.day - 1)
    }

    /** The Bikram Sambat date of [jdn]; throws [IllegalArgumentException] for days outside the years of [Int]. */
    override fun fromJdn(jdn: Jdn): CalendarDate {
        require(jdn.value in supportedDays.value) {
            "JDN ${jdn.value} is outside the Nepali years ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
        }
        val year = yearContaining(jdn.value)
        var month = MONTHS
        while (NepaliMonthStarts.startJdn(year, month) > jdn.value) month--
        val day = jdn.value - NepaliMonthStarts.startJdn(year, month) + 1
        return CalendarDate(system, year.toInt(), month, day.toInt())
    }

    private fun yearContaining(jdn: Long): Long {
        val kaliYears = floor((jdn - NepaliMonthStarts.KALI_EPOCH_JDN) / MEAN_YEAR_DAYS).toLong()
        var year = kaliYears - NepaliMonthStarts.KALI_YEAR_OFFSET
        while (startOfYear(year) > jdn) year--
        while (startOfYear(year + 1) <= jdn) year++
        return year
    }

    private fun startOfYear(year: Long): Long = NepaliMonthStarts.startJdn(year, 1)

    private fun nextMonthStart(
        year: Long,
        month: Int,
    ): Long = if (month == MONTHS) startOfYear(year + 1) else NepaliMonthStarts.startJdn(year, month + 1)
}
