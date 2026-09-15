/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoVector as libraryGeoVector
import io.github.cosinekitty.astronomy.horizon as libraryHorizon
import io.github.cosinekitty.astronomy.rotationEqjEqd as libraryRotationEqjEqd
import io.github.cosinekitty.astronomy.searchRiseSet as librarySearchRiseSet
import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Yallop's visibility classes of the new crescent, from (A) easily visible to (F) below the Danjon limit. */
public enum class CrescentVisibilityClass {
    /** Easily visible to the unaided eye. */
    A,

    /** Visible under perfect atmospheric conditions. */
    B,

    /** May need optical aid to find the crescent before it is seen with the unaided eye. */
    C,

    /** Will need optical aid to find the crescent. */
    D,

    /** Not visible with a telescope. */
    E,

    /** Not visible: below the Danjon limit. */
    F,
}

/**
 * The crescent at Yallop's best time [bestTime] on one evening: arc of light [arcLightDegrees], arc of vision
 * [arcVisionDegrees], topocentric width [widthArcMinutes], [lagMinutes] from sunset to moonset, and the test value [q].
 */
public data class CrescentObservation(
    public val bestTime: Instant,
    public val lagMinutes: Double,
    public val arcLightDegrees: Double,
    public val arcVisionDegrees: Double,
    public val widthArcMinutes: Double,
    public val q: Double,
) {
    /** Yallop's class for [q]. */
    public val visibility: CrescentVisibilityClass
        get() = Yallop.classify(q)
}

/**
 * B. D. Yallop's crescent visibility test (NAO Technical Note 69, 1997; A-06): at the best time
 * Tb = Ts + 4/9 (Tm − Ts), q = (ARCV − (11.8371 − 6.3226 W′ + 0.7319 W′² − 0.1018 W′³)) / 10,
 * with ARCV the geocentric airless altitude difference Moon − Sun and W′ the topocentric crescent width.
 * See docs/PROVENANCE.md (A-06).
 */
public object Yallop {
    private const val SEMI_DIAMETER_PER_PARALLAX = 0.27245
    private const val EARTH_EQUATORIAL_RADIUS_KM = 6_378.137
    private const val ARC_MINUTES_PER_DEGREE = 60.0
    private const val HALF_TURN = 180.0
    private const val HOURS_PER_TURN = 24.0
    private const val DEGREES_PER_HOUR = 15.0
    private val THRESHOLDS = listOf(0.216, -0.014, -0.160, -0.232, -0.293)

    /** Test value q for [arcVisionDegrees] and topocentric crescent width [widthArcMinutes]. */
    @Suppress("MagicNumber") // Coefficients of Yallop's equation (3.6).
    public fun q(
        arcVisionDegrees: Double,
        widthArcMinutes: Double,
    ): Double {
        val w = widthArcMinutes
        return (arcVisionDegrees - (11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w)) / 10
    }

    /**
     * Class of [q]: A for q > 0.216, B for q > −0.014, C for q > −0.160, D for q > −0.232, E for q > −0.293,
     * otherwise F.
     */
    public fun classify(q: Double): CrescentVisibilityClass =
        CrescentVisibilityClass.entries[THRESHOLDS.indexOfFirst { q > it }.takeIf { it >= 0 } ?: THRESHOLDS.size]

    /**
     * Topocentric crescent width W′ in arc minutes from the Moon's horizontal [parallaxArcMinutes], its geocentric
     * altitude and the arc of light: SD = 0.27245 π, SD′ = SD (1 + sin h sin π), W′ = SD′ (1 − cos ARCL).
     */
    public fun topocentricWidthArcMinutes(
        parallaxArcMinutes: Double,
        moonAltitudeDegrees: Double,
        arcLightDegrees: Double,
    ): Double {
        val semiDiameter = SEMI_DIAMETER_PER_PARALLAX * parallaxArcMinutes
        val parallax = radians(parallaxArcMinutes / ARC_MINUTES_PER_DEGREE)
        val topocentricSemiDiameter = semiDiameter * (1 + sin(radians(moonAltitudeDegrees)) * sin(parallax))
        return topocentricSemiDiameter * (1 - cos(radians(arcLightDegrees)))
    }

    /**
     * The crescent on the first evening after [from] at [place]. `null` when the Sun does not set within a day or the
     * Moon sets before the Sun (no crescent can be seen after sunset).
     */
    public fun evening(
        place: Coordinates,
        from: Instant,
    ): CrescentObservation? =
        CrescentEvening.bestTime(place, from)?.let { geometryAt(place, it.instant, it.lagMinutes) }

    private fun geometryAt(
        place: Coordinates,
        bestTime: Instant,
        lagMinutes: Double,
    ): CrescentObservation {
        val time = bestTime.toAstronomyTime()
        val sun = libraryGeoVector(Body.Sun, time, Aberration.Corrected)
        val moon = libraryGeoVector(Body.Moon, time, Aberration.Corrected)
        val toDate = libraryRotationEqjEqd(time)
        val observer = place.toObserver()

        fun altitude(vector: Vector): Double {
            val ofDate = toDate.rotate(vector)
            val rightAscensionHours = wrap(degrees(atan2(ofDate.y, ofDate.x)) / DEGREES_PER_HOUR, HOURS_PER_TURN)
            val declination = degrees(atan2(ofDate.z, hypot(ofDate.x, ofDate.y)))
            return libraryHorizon(time, observer, rightAscensionHours, declination, Refraction.None).altitude
        }
        val moonAltitude = altitude(moon)
        val arcLight = sun.angleWith(moon)
        val parallaxArcMinutes =
            degrees(asin(EARTH_EQUATORIAL_RADIUS_KM / (moon.length() * KM_PER_AU))) * ARC_MINUTES_PER_DEGREE
        val arcVision = moonAltitude - altitude(sun)
        val width = topocentricWidthArcMinutes(parallaxArcMinutes, moonAltitude, arcLight)
        return CrescentObservation(bestTime, lagMinutes, arcLight, arcVision, width, q(arcVision, width))
    }

    private fun wrap(
        value: Double,
        period: Double,
    ): Double = value - period * floor(value / period)

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}

/** Yallop's best time of an evening, Tb = Ts + 4/9 Lag, with the lag from sunset to moonset in minutes. */
internal data class BestTime(
    val instant: Instant,
    val lagMinutes: Double,
)

/** The evening a crescent test looks at, shared by Yallop's and Odeh's criteria. */
internal object CrescentEvening {
    private const val BEST_TIME_FRACTION = 4.0 / 9.0
    private const val MILLIS_PER_MINUTE = 60_000.0

    /**
     * The best time on the first evening after [from] at [place]; `null` when the Sun does not set within a day or the
     * Moon sets before the Sun.
     */
    fun bestTime(
        place: Coordinates,
        from: Instant,
    ): BestTime? {
        val observer = place.toObserver()
        val sunset = librarySearchRiseSet(Body.Sun, observer, Direction.Set, from.toAstronomyTime(), 1.0) ?: return null
        val lagMillis =
            librarySearchRiseSet(Body.Moon, observer, Direction.Set, sunset, 1.0)
                ?.let { (it.toInstant() - sunset.toInstant()).inWholeMilliseconds }
        if (lagMillis == null || lagMillis <= 0 || lagMillis >= 1.days.inWholeMilliseconds) return null
        val bestTime =
            Instant.fromEpochMilliseconds(
                sunset.toInstant().toEpochMilliseconds() + (BEST_TIME_FRACTION * lagMillis).toLong(),
            )
        return BestTime(bestTime, lagMillis / MILLIS_PER_MINUTE)
    }
}
