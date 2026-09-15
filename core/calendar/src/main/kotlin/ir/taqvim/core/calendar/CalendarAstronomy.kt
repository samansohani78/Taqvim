/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.searchSunLongitude
import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.time.Instant

/**
 * Astronomical events that calendar rules are defined by, from cosinekitty/astronomy 2.1.19 (MIT): the March equinox
 * behind Persian year starts (ADR-0026), and the conjunction, sunset, moonset and crescent geometry behind the computed
 * lunar months of the Iranian (ADR-0027) and Umm al-Qura (ADR-0028) calendars. Every calendar shares this one ephemeris;
 * library types stay inside this object.
 *
 * Moments are passed as days of the library's UT scale: days after 12:00 UTC on 1 January 2000.
 */
internal object CalendarAstronomy {
    private const val MARCH_EQUINOX_LONGITUDE = 0.0
    private const val NEW_MOON_LONGITUDE = 0.0
    private const val MARCH = 3
    private const val SEARCH_START_DAY = 10
    private const val SEARCH_DAYS = 20.0
    private const val SECONDS_PER_DAY = 86_400.0
    private const val J2000_UNIX_SECONDS = 946_728_000.0
    private const val MILLIS_PER_SECOND = 1_000.0
    private const val HALF_TURN = 180.0
    private const val HOURS_PER_TURN = 24.0
    private const val DEGREES_PER_HOUR = 15.0

    /**
     * The March equinox of Gregorian [year]: the moment the Sun's apparent ecliptic longitude reaches 0°, searched
     * from 10 March over 20 days exactly as the library's own season search does. Null when the ephemeris finds none,
     * which happens only far outside its range (tens of thousands of years).
     */
    fun marchEquinox(year: Int): Instant? =
        searchSunLongitude(MARCH_EQUINOX_LONGITUDE, Time(year, MARCH, SEARCH_START_DAY, 0, 0, 0.0), SEARCH_DAYS)
            ?.let { instantOf(it.ut) }

    /** The instant [ut] days after 12:00 UTC on 1 January 2000 (the library's UT scale), to the millisecond. */
    fun instantOf(ut: Double): Instant =
        Instant.fromEpochMilliseconds(((ut * SECONDS_PER_DAY + J2000_UNIX_SECONDS) * MILLIS_PER_SECOND).roundToLong())

    /** The UT day of [millis] milliseconds since 1970, as the library converts it. */
    fun utOfEpochMillis(millis: Long): Double = Time.fromMillisecondsSince1970(millis).ut

    /** Milliseconds since 1970 of the UT day [ut], as the library converts it. */
    fun epochMillisOf(ut: Double): Long = Time(ut).toMillisecondsSince1970()

    /** The first geocentric conjunction (new moon) within [limitDays] after [ut], or null when there is none. */
    fun newMoonAfter(
        ut: Double,
        limitDays: Double,
    ): Double? = searchMoonPhase(NEW_MOON_LONGITUDE, Time(ut), limitDays)?.ut

    /** The first sunset at [site] within [limitDays] after [ut], or null when the Sun does not set. */
    fun sunsetAfter(
        site: Coordinates,
        ut: Double,
        limitDays: Double,
    ): Double? = riseSet(Body.Sun, Direction.Set, site, ut, limitDays)

    /** The first moonset at [site] within [limitDays] after [ut], or null when the Moon does not set. */
    fun moonsetAfter(
        site: Coordinates,
        ut: Double,
        limitDays: Double,
    ): Double? = riseSet(Body.Moon, Direction.Set, site, ut, limitDays)

    /** The first moonrise at [site] within [limitDays] after [ut], or null when the Moon does not rise. */
    fun moonriseAfter(
        site: Coordinates,
        ut: Double,
        limitDays: Double,
    ): Double? = riseSet(Body.Moon, Direction.Rise, site, ut, limitDays)

    /**
     * Sun and Moon at [site] at the moment [ut] for a crescent visibility test: geocentric positions corrected for
     * aberration, their airless altitudes (equator of date, no refraction), the arc of light between them and the
     * Moon's distance.
     */
    fun crescentGeometry(
        site: Coordinates,
        ut: Double,
    ): CrescentGeometry {
        val time = Time(ut)
        val observer = observerAt(site)
        val sun = geoVector(Body.Sun, time, Aberration.Corrected)
        val moon = geoVector(Body.Moon, time, Aberration.Corrected)
        return CrescentGeometry(
            moonAltitude = airlessAltitude(time, observer, moon),
            sunAltitude = airlessAltitude(time, observer, sun),
            arcOfLight = sun.angleWith(moon),
            moonDistanceKm = moon.length() * KM_PER_AU,
        )
    }

    private fun riseSet(
        body: Body,
        direction: Direction,
        site: Coordinates,
        ut: Double,
        limitDays: Double,
    ): Double? = searchRiseSet(body, observerAt(site), direction, Time(ut), limitDays)?.ut

    private fun observerAt(site: Coordinates): Observer = Observer(site.latitude, site.longitude, site.elevationMeters)

    private fun airlessAltitude(
        time: Time,
        observer: Observer,
        vector: Vector,
    ): Double {
        val ofDate = rotationEqjEqd(time).rotate(vector)
        val rightAscension = degrees(atan2(ofDate.y, ofDate.x)) / DEGREES_PER_HOUR
        val hours = rightAscension - HOURS_PER_TURN * floor(rightAscension / HOURS_PER_TURN)
        val declination = degrees(atan2(ofDate.z, hypot(ofDate.x, ofDate.y)))
        return horizon(time, observer, hours, declination, Refraction.None).altitude
    }

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}

/**
 * Sun and Moon for a crescent visibility test (see [CalendarAstronomy.crescentGeometry]): airless altitudes and the arc
 * of light in degrees, and the Moon's geocentric distance in kilometres.
 */
internal data class CrescentGeometry(
    val moonAltitude: Double,
    val sunAltitude: Double,
    val arcOfLight: Double,
    val moonDistanceKm: Double,
)
