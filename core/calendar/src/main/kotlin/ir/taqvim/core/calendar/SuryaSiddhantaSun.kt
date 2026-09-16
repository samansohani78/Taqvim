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
 * The Sun of the Surya Siddhanta (A-07), the solar theory Nepal's national panchang uses, from the translation of
 * E. Burgess (1860): the mean Sun moves 4 320 000 revolutions in 1 577 917 828 civil days (i.29–37), its apsis 387
 * revolutions in a kalpa (i.41), and the equation of centre comes from an epicycle of 14° less 20′ at the odd
 * quadrants (ii.34, ii.38). Time is counted in civil days from the Kali epoch, midnight at Ujjain (i.45–53).
 *
 * Longitudes are sidereal, from the initial point of the Hindu sphere. Day counts are split into a whole-day [Long]
 * and a fraction, so the mean motions stay exact for every day a `Long` year count reaches.
 */
internal object SuryaSiddhantaSun {
    const val CIVIL_DAYS_IN_MAHAYUGA: Long = 1_577_917_828L
    const val SUN_REVOLUTIONS_IN_MAHAYUGA: Long = 4_320_000L
    private const val APSIS_REVOLUTIONS_IN_KALPA = 387L
    private const val MAHAYUGAS_IN_KALPA = 1_000L
    private const val CIVIL_DAYS_IN_KALPA = CIVIL_DAYS_IN_MAHAYUGA * MAHAYUGAS_IN_KALPA

    /** Civil days from the creation to the Kali epoch (Burgess i.45–53: 714 404 108 572 less 1 811 945). */
    private const val CREATION_TO_KALI_DAYS = 714_402_296_627L

    private const val SANKRANTIS_PER_YEAR = 12L
    private const val SIGN_DEGREES = 30.0
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0
    private const val EPICYCLE_DEGREES = 14.0
    private const val EPICYCLE_ODD_QUADRANT_REDUCTION = 20.0 / 60.0

    /** Half-width of the window around the mean sankranti; the equation of centre stays below 2.3°. */
    private const val SEARCH_HALF_WIDTH_DAYS = 4.0
    private const val SEARCH_STEPS = 48

    /** Mean sankranti interval, CIVIL / (12 × SUN) days, as the exact fraction [MEAN_NUMERATOR] / [MEAN_DENOMINATOR]. */
    private const val MEAN_NUMERATOR = 394_479_457L
    private const val MEAN_DENOMINATOR = 12_960_000L

    /** Sidereal longitude of the true Sun, in degrees 0‥360, [fraction] days after the start of Kali day [day]. */
    fun trueLongitude(
        day: Long,
        fraction: Double,
    ): Double {
        val mean = turns(day, fraction, SUN_REVOLUTIONS_IN_MAHAYUGA, CIVIL_DAYS_IN_MAHAYUGA)
        val apsis =
            turns(
                Math.addExact(day, CREATION_TO_KALI_DAYS),
                fraction,
                APSIS_REVOLUTIONS_IN_KALPA,
                CIVIL_DAYS_IN_KALPA,
            )
        val anomaly = (mean - apsis) * PI / HALF_TURN
        val epicycle = EPICYCLE_DEGREES - EPICYCLE_ODD_QUADRANT_REDUCTION * abs(sin(anomaly))
        val equation = asin(epicycle / FULL_TURN * sin(anomaly)) * HALF_TURN / PI
        return (mean - equation).mod(FULL_TURN)
    }

    /**
     * The instant the true Sun reaches 30° × [index] (the [index]-th sankranti after the epoch, Mesha at 0), as a Kali
     * day and the fraction of that day elapsed.
     */
    fun sankranti(index: Long): KaliInstant {
        val anchor = meanSankrantiDay(index)
        val target = (index.mod(SANKRANTIS_PER_YEAR) * SIGN_DEGREES)
        var low = -SEARCH_HALF_WIDTH_DAYS
        var high = SEARCH_HALF_WIDTH_DAYS + 1
        repeat(SEARCH_STEPS) {
            val middle = (low + high) / 2
            val behind = (trueLongitude(anchor, middle) - target + HALF_TURN).mod(FULL_TURN) - HALF_TURN < 0
            if (behind) low = middle else high = middle
        }
        val offset = (low + high) / 2
        val whole = floor(offset).toLong()
        return KaliInstant(anchor + whole, offset - whole)
    }

    /** The whole Kali day of the mean [index]-th sankranti, exactly: floor(index × numerator / denominator). */
    private fun meanSankrantiDay(index: Long): Long {
        val cycles = Math.floorDiv(index, MEAN_DENOMINATOR)
        val rest = Math.floorMod(index, MEAN_DENOMINATOR)
        return cycles * MEAN_NUMERATOR + rest * MEAN_NUMERATOR / MEAN_DENOMINATOR
    }

    /** [revolutions] per [period] days, after [day] whole days plus [fraction], in degrees 0‥360. */
    private fun turns(
        day: Long,
        fraction: Double,
        revolutions: Long,
        period: Long,
    ): Double {
        val whole = Math.floorMod(Math.multiplyExact(day, revolutions), period)
        return ((whole + fraction * revolutions) / period * FULL_TURN).mod(FULL_TURN)
    }
}

/** An instant counted from the Kali epoch: whole civil [day]s (Ujjain midnight to midnight) and the [fraction] elapsed. */
internal data class KaliInstant(
    val day: Long,
    val fraction: Double,
)
