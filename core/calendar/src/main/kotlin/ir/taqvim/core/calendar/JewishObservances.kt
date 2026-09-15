/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange

/** Jewish observances with a fixed Hebrew date (T-108), in the order they fall in a Hebrew year. */
public enum class JewishObservance {
    /** 1 Tishri. */
    ROSH_HASHANAH,

    /** 10 Tishri. */
    YOM_KIPPUR,

    /** First day of Succoth, 15 Tishri. */
    SUCCOTH,

    /** First day of Hanukkah, 25 Kislev. */
    HANUKKAH,

    /** First day of Pesach, 15 Nisan. */
    PESACH,

    /** Shavuot, 6 Sivan. */
    SHAVUOT,
}

/** An [observance] on its Hebrew [date], which is day [jdn]. */
public data class JewishObservanceDay(
    public val observance: JewishObservance,
    public val date: HebrewDate,
    public val jdn: Jdn,
)

/** Where [JewishObservance]s fall; valid for every year and day [HebrewCalendar] supports. */
public object JewishObservances {
    private const val YOM_KIPPUR_DAY = 10
    private const val FEAST_DAY = 15
    private const val HANUKKAH_DAY = 25
    private const val SHAVUOT_DAY = 6
    private const val DECEMBER = 12
    private const val LAST_DAY_OF_DECEMBER = 31

    /** The Hebrew date of [observance] in [hebrewYear]. */
    public fun date(
        observance: JewishObservance,
        hebrewYear: Int,
    ): HebrewDate =
        when (observance) {
            JewishObservance.ROSH_HASHANAH -> HebrewDate(hebrewYear, 1, 1)
            JewishObservance.YOM_KIPPUR -> HebrewDate(hebrewYear, 1, YOM_KIPPUR_DAY)
            JewishObservance.SUCCOTH -> HebrewDate(hebrewYear, 1, FEAST_DAY)
            JewishObservance.HANUKKAH -> HebrewDate(hebrewYear, month(hebrewYear, HebrewMonth.KISLEV), HANUKKAH_DAY)
            JewishObservance.PESACH -> HebrewDate(hebrewYear, month(hebrewYear, HebrewMonth.NISAN), FEAST_DAY)
            JewishObservance.SHAVUOT -> HebrewDate(hebrewYear, month(hebrewYear, HebrewMonth.SIVAN), SHAVUOT_DAY)
        }

    /** [observance] in [hebrewYear] with its day number. */
    public fun day(
        observance: JewishObservance,
        hebrewYear: Int,
    ): JewishObservanceDay {
        val date = date(observance, hebrewYear)
        return JewishObservanceDay(observance, date, HebrewCalendar.toJdn(date))
    }

    /** Every observance of [hebrewYear], in order. */
    public fun inHebrewYear(hebrewYear: Int): List<JewishObservanceDay> =
        JewishObservance.entries.map { day(it, hebrewYear) }

    /** Every observance on a day in [days], in date order. */
    public fun between(days: JdnRange): List<JewishObservanceDay> {
        if (days.isEmpty()) return emptyList()
        val firstYear = HebrewCalendar.fromJdn(days.start).year
        val lastYear = HebrewCalendar.fromJdn(days.endInclusive).year
        return (firstYear..lastYear).flatMap(::inHebrewYear).filter { it.jdn in days }
    }

    /**
     * Every observance in the proleptic Gregorian [year], in date order: zero, one or two Hanukkahs, since 25 Kislev
     * drifts from December into January over the millennia.
     */
    public fun inGregorianYear(year: Int): List<JewishObservanceDay> {
        val first = CalendarDate(CalendarSystem.GREGORIAN, year, 1, 1)
        val last = CalendarDate(CalendarSystem.GREGORIAN, year, DECEMBER, LAST_DAY_OF_DECEMBER)
        return between(GregorianCalendarSystem.toJdn(first)..GregorianCalendarSystem.toJdn(last))
    }

    private fun month(
        hebrewYear: Int,
        month: HebrewMonth,
    ): Int = HebrewCalendar.monthNumber(hebrewYear, month)
}
