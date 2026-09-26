/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Light for photographers over one day window (T-407): [goldenHours] where the Sun's apparent altitude is within
 * −4°‥6°, [blueHours] between civil twilight and an apparent −4°, and the Moon's [moonrise] and [moonset]. Lists are
 * empty when the Sun never enters that band (polar day or night); rise/set are `null` when they do not happen.
 */
public data class PhotographyDay(
    public val goldenHours: List<TimeInterval>,
    public val blueHours: List<TimeInterval>,
    public val moonrise: Instant?,
    public val moonset: Instant?,
)

/**
 * Golden and blue hours and Moon rise/set (F-10).
 *
 * The blue hour's lower edge is civil twilight, which the USNO and every other published table define as a
 * **geometric** −6°, so it is compared against [Altitudes.geometricDegrees] and the app agrees with those tables.
 * The other two edges have no published definition to agree with — they are this app's convention — and stay
 * apparent altitudes, so what a photographer sees at −4° is what the band switches on. ADR-0047 records why the two
 * edges are measured differently.
 *
 * **The golden hour's −4° and +6° are a stated convention, not a sourced quantity (ADR-0049).** No astronomical
 * authority, illumination standard or meteorological authority defines the term at all: the *Explanatory Supplement
 * to the Astronomical Almanac* names only the three twilights, the CIE's lighting vocabulary has no such entry, and
 * published photographers' ephemerides disagree with one another by up to a quarter of an hour. +6° mirrors civil
 * twilight's −6° so the two bands tile −6°‥+6°, and −4° is the single altitude that splits that interval into a warm
 * band and a blue one. The app therefore claims **no** agreement with any external ephemeris for these times; only
 * [BLUE_HOUR_BOTTOM], rise, set and transit are checked against a published table.
 */
public object PhotographyPanel {
    /** Upper altitude of golden hour, degrees: this app's convention, mirroring civil twilight (ADR-0049). */
    public const val GOLDEN_HOUR_TOP: Double = 6.0

    /** Altitude separating golden and blue hour, degrees: this app's convention, unpublished anywhere (ADR-0049). */
    public const val BLUE_HOUR_TOP: Double = -4.0

    /** Lower altitude of blue hour, degrees: civil twilight, measured geometrically as published tables define it. */
    public const val BLUE_HOUR_BOTTOM: Double = -6.0

    private val SCAN_STEP = 5.minutes
    private val PRECISION = 10.seconds

    /** Light windows for [observer] during the 24 hours starting at [from]. */
    public fun day(
        observer: Coordinates,
        from: Instant,
    ): PhotographyDay {
        val until = from + 1.days
        val altitudes = { instant: Instant -> Sky.altitudes(CelestialBody.SUN, instant, observer) }
        val moon = Sky.riseSetTransit(CelestialBody.MOON, observer, from)
        return PhotographyDay(
            goldenHours =
                IntervalSearch.intervals(from, until, SCAN_STEP, PRECISION) {
                    altitudes(it).apparentDegrees in BLUE_HOUR_TOP..GOLDEN_HOUR_TOP
                },
            blueHours =
                IntervalSearch.intervals(from, until, SCAN_STEP, PRECISION) {
                    val value = altitudes(it)
                    value.geometricDegrees >= BLUE_HOUR_BOTTOM && value.apparentDegrees < BLUE_HOUR_TOP
                },
            moonrise = moon.rise,
            moonset = moon.set,
        )
    }
}
