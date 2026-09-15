/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import kotlin.math.floor

/**
 * The astronomical rule of the Umm al-Qura calendar (A-04, ADR-0028), as described by R. H. van Gent, "The Umm
 * al-Qura Calendar of Saudi Arabia", Utrecht University (archived in `docs/sources/vangent-ummalqura/`):
 *
 * - **Since AH 1423:** if on the 29th day of the month the geocentric conjunction occurs before sunset and the Moon
 *   sets after the Sun, the next day is the first day of the new month; otherwise the month lasts 30 days.
 * - **AH 1420–1422:** the same without the conjunction condition (only moonset after sunset).
 *
 * Both are evaluated at the Kaʿba in Mecca (21.4225° N, 39.8262° E, the coordinates used for the Qibla, A-11) with
 * civil days in Saudi time (UTC+3). Events come from cosinekitty/astronomy 2.1.19 (MIT), as for the Persian year
 * starts (ADR-0026); library types stay inside this object. Days are Julian Day Numbers.
 */
internal object UmmAlQuraCriterion {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262
    private const val J2000_JDN = 2_451_545L
    private const val HALF_DAY = 0.5
    private const val SAUDI_OFFSET_DAYS = 3.0 / 24.0
    private const val NEW_MOON_LONGITUDE = 0.0
    private const val DAY_29 = 28L
    private const val SHORT_MONTH = 29L
    private const val LONG_MONTH = 30L
    private const val CONJUNCTION_LOOKBACK_DAYS = 20.0
    private const val SUN_SEARCH_DAYS = 1.0
    private const val MOON_SEARCH_DAYS = 2.0
    private const val NEW_MOON_SEARCH_BEFORE_DAYS = 8.0
    private const val NEW_MOON_SEARCH_DAYS = 12.0

    private val kaaba = Observer(KAABA_LATITUDE, KAABA_LONGITUDE, 0.0)

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
        val searchFrom = Time(approximateStart - J2000_JDN - HALF_DAY - NEW_MOON_SEARCH_BEFORE_DAYS)
        val conjunction =
            checkNotNull(searchMoonPhase(NEW_MOON_LONGITUDE, searchFrom, NEW_MOON_SEARCH_DAYS)) {
                "No conjunction near JDN $approximateStart"
            }
        return generateSequence(saudiDay(conjunction)) { it + 1 }
            .first { day -> sunset(day).let { it.ut > conjunction.ut && moonSetsAfter(it) } } + 1
    }

    private fun monthBeginsAfter(
        sunset: Time,
        requireConjunction: Boolean,
    ): Boolean = (!requireConjunction || conjunctionBefore(sunset)) && moonSetsAfter(sunset)

    /** Whether a conjunction occurred in the 20 days before [sunset] (the month's own conjunction, if any). */
    private fun conjunctionBefore(sunset: Time): Boolean =
        searchMoonPhase(NEW_MOON_LONGITUDE, sunset.addDays(-CONJUNCTION_LOOKBACK_DAYS), CONJUNCTION_LOOKBACK_DAYS) !=
            null

    /** Whether the Moon is still above the horizon at [sunset]: its next setting comes before its next rising. */
    private fun moonSetsAfter(sunset: Time): Boolean {
        val set = checkNotNull(searchRiseSet(Body.Moon, kaaba, Direction.Set, sunset, MOON_SEARCH_DAYS))
        val rise = checkNotNull(searchRiseSet(Body.Moon, kaaba, Direction.Rise, sunset, MOON_SEARCH_DAYS))
        return set.ut < rise.ut
    }

    /** Sunset at the Kaʿba on the civil day [jdn] (Saudi time). */
    private fun sunset(jdn: Long): Time {
        val saudiMidnight = Time(jdn - J2000_JDN - HALF_DAY - SAUDI_OFFSET_DAYS)
        return checkNotNull(searchRiseSet(Body.Sun, kaaba, Direction.Set, saudiMidnight, SUN_SEARCH_DAYS)) {
            "No sunset at the Kaaba on JDN $jdn"
        }
    }

    /** The civil day (Saudi time) containing [time]. */
    private fun saudiDay(time: Time): Long = floor(time.ut + HALF_DAY + SAUDI_OFFSET_DAYS).toLong() + J2000_JDN
}
