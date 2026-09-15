/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Coordinates
import kotlin.math.floor

/**
 * The astronomical rule of the Umm al-Qura calendar (A-04, ADR-0028), as described by R. H. van Gent, "The Umm
 * al-Qura Calendar of Saudi Arabia", Utrecht University, https://webspace.science.uu.nl/~gent0113/islam/ummalqura.htm
 * (rules page `ummalqura_rules.htm`; retrieved 2026-09-15; SHA-256 in docs/sources/MANIFEST.md):
 *
 * - **Since AH 1423:** if on the 29th day of the month the geocentric conjunction occurs before sunset and the Moon
 *   sets after the Sun, the next day is the first day of the new month; otherwise the month lasts 30 days.
 * - **AH 1420–1422:** the same without the conjunction condition (only moonset after sunset).
 *
 * Both are evaluated at the Kaʿba in Mecca (21.4225° N, 39.8262° E, the coordinates used for the Qibla, A-11) with
 * civil days in Saudi time (UTC+3). Events come from [CalendarAstronomy], as for the Persian year starts (ADR-0026).
 * Days are Julian Day Numbers; moments are UT days of [CalendarAstronomy].
 */
internal object UmmAlQuraCriterion {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262
    private const val J2000_JDN = 2_451_545L
    private const val HALF_DAY = 0.5
    private const val SAUDI_OFFSET_DAYS = 3.0 / 24.0
    private const val DAY_29 = 28L
    private const val SHORT_MONTH = 29L
    private const val LONG_MONTH = 30L
    private const val CONJUNCTION_LOOKBACK_DAYS = 20.0
    private const val SUN_SEARCH_DAYS = 1.0
    private const val MOON_SEARCH_DAYS = 2.0
    private const val NEW_MOON_SEARCH_BEFORE_DAYS = 8.0
    private const val NEW_MOON_SEARCH_DAYS = 12.0

    private val kaaba = Coordinates(KAABA_LATITUDE, KAABA_LONGITUDE)

    /**
     * First day of the month after the month that began on [monthStart]: the day after its 29th day when the rule
     * holds at sunset of that day, else the day after its 30th. [requireConjunction] selects the rule since AH 1423
     * (`true`) or the AH 1420–1422 rule (`false`).
     */
    fun nextMonthStart(
        monthStart: Long,
        requireConjunction: Boolean = true,
    ): Long =
        if (monthBeginsAfter(sunset(monthStart + DAY_29), requireConjunction)) {
            monthStart + SHORT_MONTH
        } else {
            monthStart + LONG_MONTH
        }

    /**
     * First day of the month whose conjunction lies near [approximateStart] (a fractional JDN within a few days of the
     * month's first day): the day after the first evening, from the day of the conjunction on, on which the conjunction
     * has occurred before sunset and the Moon sets after the Sun.
     */
    fun monthStartNear(approximateStart: Double): Long {
        val searchFrom = approximateStart - J2000_JDN - HALF_DAY - NEW_MOON_SEARCH_BEFORE_DAYS
        val conjunction =
            checkNotNull(CalendarAstronomy.newMoonAfter(searchFrom, NEW_MOON_SEARCH_DAYS)) {
                "No conjunction near JDN $approximateStart"
            }
        return generateSequence(saudiDay(conjunction)) { it + 1 }
            .first { day -> sunset(day).let { it > conjunction && moonSetsAfter(it) } } + 1
    }

    private fun monthBeginsAfter(
        sunset: Double,
        requireConjunction: Boolean,
    ): Boolean = (!requireConjunction || conjunctionBefore(sunset)) && moonSetsAfter(sunset)

    /** Whether a conjunction occurred in the 20 days before [sunset] (the month's own conjunction, if any). */
    private fun conjunctionBefore(sunset: Double): Boolean =
        CalendarAstronomy.newMoonAfter(sunset - CONJUNCTION_LOOKBACK_DAYS, CONJUNCTION_LOOKBACK_DAYS) != null

    /** Whether the Moon is still above the horizon at [sunset]: its next setting comes before its next rising. */
    private fun moonSetsAfter(sunset: Double): Boolean {
        val set = checkNotNull(CalendarAstronomy.moonsetAfter(kaaba, sunset, MOON_SEARCH_DAYS))
        val rise = checkNotNull(CalendarAstronomy.moonriseAfter(kaaba, sunset, MOON_SEARCH_DAYS))
        return set < rise
    }

    /** Sunset at the Kaʿba on the civil day [jdn] (Saudi time). */
    private fun sunset(jdn: Long): Double {
        val saudiMidnight = jdn - J2000_JDN - HALF_DAY - SAUDI_OFFSET_DAYS
        return checkNotNull(CalendarAstronomy.sunsetAfter(kaaba, saudiMidnight, SUN_SEARCH_DAYS)) {
            "No sunset at the Kaaba on JDN $jdn"
        }
    }

    /** The civil day (Saudi time) containing [time]. */
    private fun saudiDay(time: Double): Long = floor(time + HALF_DAY + SAUDI_OFFSET_DAYS).toLong() + J2000_JDN
}
