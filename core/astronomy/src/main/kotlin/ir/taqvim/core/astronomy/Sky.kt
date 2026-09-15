/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.ApsisKind as LibraryApsisKind
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon as libraryEclipticMoon
import io.github.cosinekitty.astronomy.equator as libraryEquator
import io.github.cosinekitty.astronomy.helioDistance as libraryHelioDistance
import io.github.cosinekitty.astronomy.horizon as libraryHorizon
import io.github.cosinekitty.astronomy.illumination as libraryIllumination
import io.github.cosinekitty.astronomy.libration as libraryLibration
import io.github.cosinekitty.astronomy.moonPhase as libraryMoonPhase
import io.github.cosinekitty.astronomy.nextMoonQuarter as libraryNextMoonQuarter
import io.github.cosinekitty.astronomy.planetApsidesAfter as libraryPlanetApsidesAfter
import io.github.cosinekitty.astronomy.searchHourAngle as librarySearchHourAngle
import io.github.cosinekitty.astronomy.searchMoonQuarter as librarySearchMoonQuarter
import io.github.cosinekitty.astronomy.searchRiseSet as librarySearchRiseSet
import io.github.cosinekitty.astronomy.seasons as librarySeasons
import io.github.cosinekitty.astronomy.siderealTime as librarySiderealTime
import io.github.cosinekitty.astronomy.sunPosition as librarySunPosition
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

internal fun Instant.toAstronomyTime(): Time = Time.fromMillisecondsSince1970(toEpochMilliseconds())

internal fun Time.toInstant(): Instant = Instant.fromEpochMilliseconds(toMillisecondsSince1970())

internal fun Coordinates.toObserver(): Observer = Observer(latitude, longitude, elevationMeters)

private fun CelestialBody.toBody(): Body =
    when (this) {
        CelestialBody.SUN -> Body.Sun
        CelestialBody.MOON -> Body.Moon
    }

/**
 * Astronomy façade (A-13) over cosinekitty/astronomy 2.1.19 (MIT): seasons, Moon phases, rise/set/transit, positions
 * and libration in Taqvim types. Eclipses are in [Eclipses].
 */
public object Sky {
    private const val SEARCH_DAYS = 1.0
    private const val HALF_TURN = 180.0
    private const val HOUR_DEGREES = 15.0

    /** Equinoxes and solstices of Gregorian [year]. */
    public fun seasons(year: Int): SeasonInstants =
        librarySeasons(year).let {
            SeasonInstants(
                marchEquinox = it.marchEquinox.toInstant(),
                juneSolstice = it.juneSolstice.toInstant(),
                septemberEquinox = it.septemberEquinox.toInstant(),
                decemberSolstice = it.decemberSolstice.toInstant(),
            )
        }

    /**
     * Perihelia and aphelia of the Earth's center from [from] (inclusive) until [until] (exclusive), in time order.
     *
     * The distance is the Earth's center to the Sun's center, not the Earth–Moon barycenter (whose apsides differ by up
     * to about a day and a half). Near an apsis the distance changes very slowly, so a distance error of a few
     * kilometres moves the instant by up to about two hours. The monthly lunar wobble of the Earth's center never adds
     * a second extremum while the orbit's curvature term a·e·n² exceeds the wobble's (true while e > 0.005, i.e. for
     * tens of millennia around the present); accuracy beyond that follows the astronomy library's ephemeris.
     */
    public fun earthApsides(
        from: Instant,
        until: Instant,
    ): List<EarthApsis> {
        require(from < until) { "from must be before until" }
        return libraryPlanetApsidesAfter(Body.Earth, from.toAstronomyTime())
            .takeWhile { it.time.toInstant() < until }
            .map { EarthApsis(it.kind.toApsisKind(), it.time.toInstant(), it.distanceAu) }
            .toList()
    }

    /** Distance between the centers of the Earth and the Sun at [instant], in au. */
    internal fun earthSunDistanceAu(instant: Instant): Double =
        libraryHelioDistance(Body.Earth, instant.toAstronomyTime())

    private fun LibraryApsisKind.toApsisKind(): ApsisKind =
        when (this) {
            LibraryApsisKind.Pericenter -> ApsisKind.PERIHELION
            LibraryApsisKind.Apocenter -> ApsisKind.APHELION
        }

    /** Moon's ecliptic longitude east of the Sun at [instant]: 0 new, 90 first quarter, 180 full, 270 third quarter. */
    public fun moonPhaseDegrees(instant: Instant): Double = libraryMoonPhase(instant.toAstronomyTime())

    /** Principal Moon phases from [from] (inclusive) until [until] (exclusive), in time order. */
    public fun moonQuarters(
        from: Instant,
        until: Instant,
    ): List<MoonPhaseEvent> {
        require(from < until) { "from must be before until" }
        return generateSequence(librarySearchMoonQuarter(from.toAstronomyTime())) { libraryNextMoonQuarter(it) }
            .takeWhile { it.time.toInstant() < until }
            .map { MoonPhaseEvent(MoonQuarter.entries[it.quarter], it.time.toInstant()) }
            .toList()
    }

    /** The Moon's appearance at [instant] for an [observer], mirrored in the southern hemisphere. */
    public fun moonAppearance(
        instant: Instant,
        observer: Coordinates,
    ): MoonAppearance {
        val time = instant.toAstronomyTime()
        val waxing = libraryMoonPhase(time) < HALF_TURN
        return MoonAppearance(
            illuminatedFraction = libraryIllumination(Body.Moon, time).phaseFraction,
            waxing = waxing,
            brightLimbOnRight = waxing == (observer.latitude >= 0),
        )
    }

    /**
     * How the Moon's bright limb is turned for [observer] at [instant], from topocentric apparent coordinates of date
     * (Meeus eqs. 48.5 and 14.1). The illuminated fraction is in [moonAppearance].
     */
    public fun moonTilt(
        instant: Instant,
        observer: Coordinates,
    ): MoonTilt {
        val time = instant.toAstronomyTime()
        val place = observer.toObserver()
        val sun = libraryEquator(Body.Sun, time, place, EquatorEpoch.OfDate, Aberration.Corrected)
        val moon = libraryEquator(Body.Moon, time, place, EquatorEpoch.OfDate, Aberration.Corrected)
        val positionAngle =
            LunarLimb.brightLimbPositionAngle(sun.ra * HOUR_DEGREES, sun.dec, moon.ra * HOUR_DEGREES, moon.dec)
        val hourAngle = (librarySiderealTime(time) - moon.ra) * HOUR_DEGREES + observer.longitude
        val parallactic = LunarLimb.parallacticAngle(hourAngle, observer.latitude, moon.dec)
        return MoonTilt(positionAngle, parallactic, LunarLimb.signed(positionAngle - parallactic))
    }

    /** The first rise, upper transit and set of [body] for [observer] within one day after [from]. */
    public fun riseSetTransit(
        body: CelestialBody,
        observer: Coordinates,
        from: Instant,
    ): RiseSetTransit {
        val place = observer.toObserver()
        val start = from.toAstronomyTime()
        val libraryBody = body.toBody()
        val transit = librarySearchHourAngle(libraryBody, place, 0.0, start, 1).time.toInstant()
        return RiseSetTransit(
            rise = librarySearchRiseSet(libraryBody, place, Direction.Rise, start, SEARCH_DAYS)?.toInstant(),
            transit = transit.takeIf { it < from + 1.days },
            set = librarySearchRiseSet(libraryBody, place, Direction.Set, start, SEARCH_DAYS)?.toInstant(),
        )
    }

    /** Geocentric ecliptic position of [body] at [instant]. */
    public fun eclipticPosition(
        body: CelestialBody,
        instant: Instant,
    ): EclipticPosition {
        val time = instant.toAstronomyTime()
        return when (body) {
            CelestialBody.SUN -> librarySunPosition(time).let { EclipticPosition(it.elat, it.elon) }
            CelestialBody.MOON -> libraryEclipticMoon(time).let { EclipticPosition(it.lat, it.lon) }
        }
    }

    /** Where [body] appears in the sky of [observer] at [instant]. */
    public fun skyPosition(
        body: CelestialBody,
        instant: Instant,
        observer: Coordinates,
    ): SkyPosition {
        val time = instant.toAstronomyTime()
        val place = observer.toObserver()
        val equatorial = libraryEquator(body.toBody(), time, place, EquatorEpoch.OfDate, Aberration.Corrected)
        val horizontal = libraryHorizon(time, place, equatorial.ra, equatorial.dec, Refraction.Normal)
        return SkyPosition(horizontal.azimuth, horizontal.altitude, equatorial.ra, equatorial.dec)
    }

    /** Optical libration of the Moon at [instant]; longitude in −180°‥180°. */
    public fun libration(instant: Instant): Libration =
        libraryLibration(instant.toAstronomyTime()).let {
            // elat/elon are the libration angles; mlat/mlon are the Moon's ecliptic coordinates.
            Libration(it.elat, (it.elon + HALF_TURN).mod(2 * HALF_TURN) - HALF_TURN, it.distanceKm, it.diamDeg)
        }
}
