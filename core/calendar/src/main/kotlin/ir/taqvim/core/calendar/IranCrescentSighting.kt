/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Iran calibration of the calculated-observational crescent test (A-06, ADR-0009 addendum, ADR-0027): the crescent
 * counts as seen on an evening when B. D. Yallop's test (NAO Technical Note 69, 1997, eq. 3.6) rates it class D or
 * better (q > −0.232) at any of five reference cities across Iran. This is the same computation as
 * `IranCrescentCalibration` in `:core:astronomy`, repeated here because `:core:calendar` cannot depend on that module;
 * a test there checks that both give the same months. Sun and Moon come from [CalendarAstronomy].
 */
internal object IranCrescentSighting {
    /** Tehran, Mashhad, Zahedan, Bandar Abbas and Shiraz, with the coordinates stated in their official timetables. */
    val SITES: List<Coordinates> =
        listOf(
            Coordinates(35.70, 51.42),
            Coordinates(36.29, 59.62),
            Coordinates(29.50, 60.86),
            Coordinates(27.18, 56.28),
            Coordinates(29.61, 52.53),
        )

    /** Lower bound of Yallop's class D: a crescent with a larger q is class D or better. */
    private const val CLASS_D_LOWER_Q = -0.232
    private const val BEST_TIME_FRACTION = 4.0 / 9.0
    private const val SEMI_DIAMETER_PER_PARALLAX = 0.27245
    private const val EARTH_EQUATORIAL_RADIUS_KM = 6_378.137
    private const val ARC_MINUTES_PER_DEGREE = 60.0
    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0
    private const val SEARCH_DAYS = 1.0
    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_587.5
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Whether any site sees the new crescent on the evening of the civil day [jdn]. */
    fun seenOnEvening(jdn: Long): Boolean = SITES.any { place -> q(place, jdn)?.let { it > CLASS_D_LOWER_Q } ?: false }

    /**
     * Yallop's q at [place] on the first evening after local mean noon of [jdn], at the best time
     * Tb = Ts + 4/9 (Tm − Ts). Null when the Sun does not set within a day or the Moon sets before the Sun.
     */
    private fun q(
        place: Coordinates,
        jdn: Long,
    ): Double? {
        val noon = CalendarAstronomy.utOfEpochMillis(localNoonMillis(jdn, place.longitude))
        val sunset = CalendarAstronomy.sunsetAfter(place, noon, SEARCH_DAYS)
        val moonset = sunset?.let { CalendarAstronomy.moonsetAfter(place, it, SEARCH_DAYS) }
        if (sunset == null || moonset == null) return null
        val sunsetMillis = CalendarAstronomy.epochMillisOf(sunset)
        val lagMillis = CalendarAstronomy.epochMillisOf(moonset) - sunsetMillis
        val bestTime = CalendarAstronomy.utOfEpochMillis(sunsetMillis + (BEST_TIME_FRACTION * lagMillis).toLong())
        return if (lagMillis <= 0 || lagMillis >= MILLIS_PER_DAY) null else qAt(place, bestTime)
    }

    private fun qAt(
        place: Coordinates,
        ut: Double,
    ): Double {
        val sky = CalendarAstronomy.crescentGeometry(place, ut)
        val parallax = asin(EARTH_EQUATORIAL_RADIUS_KM / sky.moonDistanceKm)
        val semiDiameter = SEMI_DIAMETER_PER_PARALLAX * degrees(parallax) * ARC_MINUTES_PER_DEGREE
        val topocentricSemiDiameter = semiDiameter * (1 + sin(radians(sky.moonAltitude)) * sin(parallax))
        val width = topocentricSemiDiameter * (1 - cos(radians(sky.arcOfLight)))
        return yallopQ(sky.moonAltitude - sky.sunAltitude, width)
    }

    /** Yallop's equation (3.6) for the arc of vision [arcVision] (degrees) and crescent width [width] (arc minutes). */
    @Suppress("MagicNumber") // Coefficients of Yallop's equation (3.6).
    private fun yallopQ(
        arcVision: Double,
        width: Double,
    ): Double = (arcVision - (11.8371 - 6.3226 * width + 0.7319 * width * width - 0.1018 * width * width * width)) / 10

    /** Mean local noon at [longitude] on the civil day [jdn], in milliseconds since 1970. */
    private fun localNoonMillis(
        jdn: Long,
        longitude: Double,
    ): Long = ((jdn - longitude / FULL_TURN - JULIAN_DAY_OF_UNIX_EPOCH) * MILLIS_PER_DAY).toLong()

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
