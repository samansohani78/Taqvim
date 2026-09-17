/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.sin

/**
 * The Moon of the Surya Siddhanta (ADR-0038), from the translation of E. Burgess (1860): the mean Moon moves
 * 57 753 336 revolutions and its apsis 488 203 revolutions in a mahayuga of 1 577 917 828 civil days (chapter i), the
 * apsis stands at 90° at the Kali epoch while the mean Sun and Moon are at 0° (chapter i), and the equation of centre
 * comes from an epicycle of 32° less 20′ at the odd quadrants (chapter ii). Time is counted as in
 * [SuryaSiddhantaSun]. Checked against every tithi-dated holiday of BS 2082 and 2083 in `NepaliLunarDaysTest`.
 */
internal object SuryaSiddhantaMoon {
    private const val MOON_REVOLUTIONS_IN_MAHAYUGA = 57_753_336L
    private const val APSIS_REVOLUTIONS_IN_MAHAYUGA = 488_203L
    private const val APSIS_AT_EPOCH = 90.0
    private const val EPICYCLE_DEGREES = 32.0
    private const val EPICYCLE_ODD_QUADRANT_REDUCTION = 20.0 / 60.0
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    /** Sidereal longitude of the true Moon, in degrees 0‥360, [fraction] days after the start of Kali day [day]. */
    fun trueLongitude(
        day: Long,
        fraction: Double,
    ): Double {
        val mean = turns(day, fraction, MOON_REVOLUTIONS_IN_MAHAYUGA)
        val apsis = APSIS_AT_EPOCH + turns(day, fraction, APSIS_REVOLUTIONS_IN_MAHAYUGA)
        val anomaly = (mean - apsis) * PI / HALF_TURN
        val epicycle = EPICYCLE_DEGREES - EPICYCLE_ODD_QUADRANT_REDUCTION * abs(sin(anomaly))
        val equation = asin(epicycle / FULL_TURN * sin(anomaly)) * HALF_TURN / PI
        return (mean - equation).mod(FULL_TURN)
    }

    /** The Moon's distance east of the true Sun, in degrees 0‥360, at [offset] days after the start of Kali day [base]. */
    fun elongation(
        base: Long,
        offset: Double,
    ): Double {
        val whole = floor(offset)
        val day = Math.addExact(base, whole.toLong())
        val fraction = offset - whole
        return (trueLongitude(day, fraction) - SuryaSiddhantaSun.trueLongitude(day, fraction)).mod(FULL_TURN)
    }

    /**
     * [revolutions] per mahayuga after [day] whole days plus [fraction], in degrees 0‥360. The day is reduced modulo the
     * mahayuga first, so the product stays within [Long] for every day.
     */
    private fun turns(
        day: Long,
        fraction: Double,
        revolutions: Long,
    ): Double {
        val period = SuryaSiddhantaSun.CIVIL_DAYS_IN_MAHAYUGA
        val whole = Math.floorMod(Math.floorMod(day, period) * revolutions, period)
        return ((whole + fraction * revolutions) / period * FULL_TURN).mod(FULL_TURN)
    }
}
