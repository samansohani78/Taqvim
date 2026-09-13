/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.constellation as libraryConstellation
import io.github.cosinekitty.astronomy.geoVector as libraryGeoVector
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The twelve tropical signs, each 30° of ecliptic longitude starting at the March equinox point. */
public enum class ZodiacSign {
    ARIES,
    TAURUS,
    GEMINI,
    CANCER,
    LEO,
    VIRGO,
    LIBRA,
    SCORPIO,
    SAGITTARIUS,
    CAPRICORN,
    AQUARIUS,
    PISCES,
    ;

    public companion object {
        private const val SIGN_WIDTH_DEGREES = 30.0
        private const val FULL_TURN = 360.0

        /** The tropical sign containing ecliptic longitude [degrees] (any real value; wrapped into 0‥360). */
        public fun ofEclipticLongitude(degrees: Double): ZodiacSign {
            val wrapped = degrees - FULL_TURN * floor(degrees / FULL_TURN)
            return entries[(wrapped / SIGN_WIDTH_DEGREES).toInt().coerceAtMost(entries.size - 1)]
        }
    }
}

/** How "the Moon in Scorpio" is decided. */
public enum class ZodiacSystem {
    /** Tropical sign Scorpio: ecliptic longitude 210°‥240°. */
    TROPICAL,

    /** The IAU constellation Scorpius, with official boundaries. */
    IAU_CONSTELLATION,
}

/** A half-open interval of time from [start] until [end]. */
public data class TimeInterval(
    public val start: Instant,
    public val end: Instant,
)

/** Zodiac signs, IAU constellations and the "Moon in Scorpio" search (T-404) on the astronomy façade. */
public object Zodiac {
    /** IAU abbreviation of Scorpius. */
    public const val SCORPIUS: String = "Sco"

    private val SCAN_STEP = 1.hours
    private val PRECISION = 1.minutes
    private const val DEGREES_PER_HOUR = 15.0
    private const val HOURS_PER_TURN = 24.0
    private const val HALF_TURN = 180.0

    /** Tropical sign of [body] at [instant]. */
    public fun tropicalSign(
        body: CelestialBody,
        instant: Instant,
    ): ZodiacSign = ZodiacSign.ofEclipticLongitude(Sky.eclipticPosition(body, instant).longitudeDegrees)

    /** IAU constellation abbreviation (e.g. "Sco") containing J2000 [rightAscensionHours], [declinationDegrees]. */
    public fun iauConstellation(
        rightAscensionHours: Double,
        declinationDegrees: Double,
    ): String = libraryConstellation(rightAscensionHours, declinationDegrees).symbol

    /** IAU constellation of the Moon at [instant], from its geocentric J2000 position. */
    public fun moonConstellation(instant: Instant): String {
        val moon = libraryGeoVector(Body.Moon, instant.toAstronomyTime(), Aberration.Corrected)
        val rightAscension = atan2(moon.y, moon.x) * HALF_TURN / PI / DEGREES_PER_HOUR
        val declination = atan2(moon.z, hypot(moon.x, moon.y)) * HALF_TURN / PI
        return iauConstellation(rightAscension - HOURS_PER_TURN * floor(rightAscension / HOURS_PER_TURN), declination)
    }

    /**
     * Intervals within [from] until [until] during which the Moon is in Scorpio under [system]. The window is scanned
     * hourly (visits shorter than an hour may be missed) and boundaries are refined to a minute.
     */
    public fun moonInScorpio(
        from: Instant,
        until: Instant,
        system: ZodiacSystem = ZodiacSystem.IAU_CONSTELLATION,
    ): List<TimeInterval> {
        require(from < until) { "from must be before until" }
        val inScorpio: (Instant) -> Boolean =
            when (system) {
                ZodiacSystem.TROPICAL -> { instant -> tropicalSign(CelestialBody.MOON, instant) == ZodiacSign.SCORPIO }
                ZodiacSystem.IAU_CONSTELLATION -> { instant -> moonConstellation(instant) == SCORPIUS }
            }
        val intervals = mutableListOf<TimeInterval>()
        var openedAt: Instant? = if (inScorpio(from)) from else null
        var time = from
        while (time < until) {
            val next = minOf(time + SCAN_STEP, until)
            if (inScorpio(next) != (openedAt != null)) {
                val boundary = refine(time, next, inScorpio)
                openedAt =
                    if (openedAt == null) boundary else null.also { intervals += TimeInterval(openedAt, boundary) }
            }
            time = next
        }
        openedAt?.let { intervals += TimeInterval(it, until) }
        return intervals
    }

    /** First instant (to [PRECISION]) after [before] at which [predicate] takes the value it has at [after]. */
    private fun refine(
        before: Instant,
        after: Instant,
        predicate: (Instant) -> Boolean,
    ): Instant {
        val target = predicate(after)
        var low = before
        var high = after
        while (high - low > PRECISION) {
            val middle = low + (high - low) / 2
            if (predicate(middle) == target) high = middle else low = middle
        }
        return high
    }
}
