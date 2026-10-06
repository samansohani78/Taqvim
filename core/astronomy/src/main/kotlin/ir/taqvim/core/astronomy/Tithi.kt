/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.calendar.Paksha
import ir.taqvim.core.calendar.TithiPosition
import kotlin.math.floor
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Tithi (A-15, T-406): the Moon's elongation from the Sun in ecliptic longitude, in 12° steps. This is a modern
 * ephemeris approximation from true geocentric longitudes (cosinekitty via [Sky.moonPhaseDegrees]); it is not the
 * traditional Surya Siddhanta mean/true computation, whose tithi boundaries can differ by hours.
 *
 * **Not the value the app displays.** DT-043: the screen shows
 * [ir.taqvim.core.calendar.SuryaSiddhantaTithi], the convention Nepal's panchang and this app's own Nepali festivals
 * follow, so a tithi on screen never contradicts a festival the app places elsewhere. This one stays as the modern
 * reading of the same quantity, and as what the two conventions are compared with.
 */
public object Tithi {
    private const val DEGREES_PER_TITHI = 12.0
    private const val TITHIS_PER_MONTH = 30
    private const val TITHIS_PER_PAKSHA = 15
    private const val FULL_TURN = 360.0
    private val SCAN_STEP = 2.hours
    private val PRECISION = 30.seconds

    /** Tithi for the Moon–Sun elongation [elongationDegrees] (any real value). */
    public fun ofElongation(elongationDegrees: Double): TithiPosition {
        val wrapped = elongationDegrees - FULL_TURN * floor(elongationDegrees / FULL_TURN)
        val number = (wrapped / DEGREES_PER_TITHI).toInt().coerceAtMost(TITHIS_PER_MONTH - 1) + 1
        return TithiPosition(number, if (number <= TITHIS_PER_PAKSHA) Paksha.SHUKLA else Paksha.KRISHNA)
    }

    /** Tithi at [instant]. */
    public fun at(instant: Instant): TithiPosition = ofElongation(Sky.moonPhaseDegrees(instant))

    /** Instants in [from] until [until] at which a new tithi begins (to about 30 seconds), in order. */
    public fun changes(
        from: Instant,
        until: Instant,
    ): List<Instant> {
        require(from < until) { "from must be before until" }
        val changes = mutableListOf<Instant>()
        var time = from
        var current = at(from).number
        while (time < until) {
            val next = minOf(time + SCAN_STEP, until)
            val tithiThere = at(next).number
            if (tithiThere != current) {
                changes += IntervalSearch.boundary(time, next, PRECISION) { at(it).number == tithiThere }
                current = tithiThere
            }
            time = next
        }
        return changes
    }
}
