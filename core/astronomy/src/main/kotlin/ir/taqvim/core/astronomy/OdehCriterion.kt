/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.equator as libraryEquator
import io.github.cosinekitty.astronomy.geoVector as libraryGeoVector
import io.github.cosinekitty.astronomy.horizon as libraryHorizon
import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.time.Instant

/** Odeh's visibility zones of the new crescent, from (A) visible to the naked eye to (D) not visible. */
public enum class OdehZone {
    /** Visible by naked eyes. */
    A,

    /** Visible by optical aid, and could be seen by naked eyes. */
    B,

    /** Visible by optical aid only. */
    C,

    /** Not visible even by optical aid. */
    D,
}

/**
 * The crescent at the best time [bestTime] of one evening in Odeh's terms, all topocentric: airless arc of vision
 * [arcVisionDegrees], arc of light [arcLightDegrees], crescent width [widthArcMinutes], [lagMinutes] from sunset to
 * moonset, and the visibility value [v].
 */
public data class OdehObservation(
    public val bestTime: Instant,
    public val lagMinutes: Double,
    public val arcLightDegrees: Double,
    public val arcVisionDegrees: Double,
    public val widthArcMinutes: Double,
    public val v: Double,
) {
    /** Odeh's zone for [v]. */
    public val zone: OdehZone
        get() = Odeh.classify(v)
}

/**
 * M. Sh. Odeh's crescent visibility criterion ("New Criterion for Lunar Crescent Visibility", Experimental Astronomy
 * 18: 39–64, 2004, equation 2 and Table V): at the best time Tb = Ts + 4/9 Lag,
 * V = ARCV − (−0.1018 W³ + 0.7319 W² − 6.3226 W + 7.1651), with ARCV the airless topocentric arc of vision in degrees
 * and W the topocentric crescent width in arc minutes. See docs/PROVENANCE.md.
 */
public object Odeh {
    private const val ZONE_A = 5.65
    private const val ZONE_B = 2.0
    private const val ZONE_C = -0.96
    private const val SEMI_DIAMETER_PER_PARALLAX = 0.27245
    private const val EARTH_EQUATORIAL_RADIUS_KM = 6_378.137
    private const val ARC_MINUTES_PER_DEGREE = 60.0
    private const val HALF_TURN = 180.0

    /** Visibility value V for the topocentric [arcVisionDegrees] and [widthArcMinutes] (equation 2). */
    @Suppress("MagicNumber") // Coefficients of Odeh's equation (2).
    public fun v(
        arcVisionDegrees: Double,
        widthArcMinutes: Double,
    ): Double {
        val w = widthArcMinutes
        return arcVisionDegrees - (-0.1018 * w * w * w + 0.7319 * w * w - 6.3226 * w + 7.1651)
    }

    /** Zone of [v]: A for V ≥ 5.65, B for V ≥ 2, C for V ≥ −0.96, otherwise D. */
    public fun classify(v: Double): OdehZone =
        when {
            v >= ZONE_A -> OdehZone.A
            v >= ZONE_B -> OdehZone.B
            v >= ZONE_C -> OdehZone.C
            else -> OdehZone.D
        }

    /**
     * The crescent on the first evening after [from] at [place]. `null` when the Sun does not set within a day or the
     * Moon sets before the Sun.
     */
    public fun evening(
        place: Coordinates,
        from: Instant,
    ): OdehObservation? = CrescentEvening.bestTime(place, from)?.let { geometryAt(place, it) }

    private fun geometryAt(
        place: Coordinates,
        best: BestTime,
    ): OdehObservation {
        val time = best.instant.toAstronomyTime()
        val observer = place.toObserver()
        val sun = libraryEquator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moon = libraryEquator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)

        fun altitude(body: Equatorial): Double =
            libraryHorizon(time, observer, body.ra, body.dec, Refraction.None).altitude
        val arcVision = altitude(moon) - altitude(sun)
        val arcLight = sun.vec.angleWith(moon.vec)
        // The semi-diameter seen from the place grows as the Moon's distance shrinks from geocentric to topocentric.
        val geocentricDistance = libraryGeoVector(Body.Moon, time, Aberration.Corrected).length()
        val parallaxArcMinutes =
            degrees(asin(EARTH_EQUATORIAL_RADIUS_KM / (geocentricDistance * KM_PER_AU))) * ARC_MINUTES_PER_DEGREE
        val semiDiameter = SEMI_DIAMETER_PER_PARALLAX * parallaxArcMinutes * geocentricDistance / moon.dist
        val width = semiDiameter * (1 - cos(radians(arcLight)))
        return OdehObservation(best.instant, best.lagMinutes, arcLight, arcVision, width, v(arcVision, width))
    }

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
