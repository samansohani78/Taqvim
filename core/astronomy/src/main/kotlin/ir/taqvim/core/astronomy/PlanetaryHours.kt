/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Weekday
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The seven classical planets in Chaldean order (slowest apparent motion first). */
public enum class ClassicalPlanet {
    SATURN,
    JUPITER,
    MARS,
    SUN,
    VENUS,
    MERCURY,
    MOON,
}

/**
 * One of the 24 unequal planetary hours of a day: hour [number] (1‥24) from [start] until [end],
 * ruled by [ruler].
 */
public data class PlanetaryHour(
    public val number: Int,
    public val start: Instant,
    public val end: Instant,
    public val ruler: ClassicalPlanet,
    public val daytime: Boolean,
)

/** Planetary hours of a day, or why they cannot be formed. */
public sealed interface PlanetaryHoursResult {
    /** The 24 hours from sunrise to the next sunrise. */
    public data class Available(
        public val hours: List<PlanetaryHour>,
    ) : PlanetaryHoursResult

    /** The Sun does not both rise and set (polar day or night) within the searched day. */
    public data object Unavailable : PlanetaryHoursResult
}

/**
 * Planetary hours (T-406): daylight (sunrise→sunset) and night (sunset→next sunrise) are each split into 12 equal
 * parts. The rulers follow the Chaldean order, and the first hour of each weekday is ruled by that day's planet
 * (Sunday–Sun, Monday–Moon, Tuesday–Mars, Wednesday–Mercury, Thursday–Jupiter, Friday–Venus, Saturday–Saturn).
 */
public object PlanetaryHours {
    private const val HOURS_PER_HALF = 12
    private const val HOURS_PER_DAY = 24
    private val MIN_PERIOD = 1.minutes

    /** Planet ruling the first hour of [weekday]. */
    public fun rulerOfDay(weekday: Weekday): ClassicalPlanet =
        when (weekday) {
            Weekday.SUNDAY -> ClassicalPlanet.SUN
            Weekday.MONDAY -> ClassicalPlanet.MOON
            Weekday.TUESDAY -> ClassicalPlanet.MARS
            Weekday.WEDNESDAY -> ClassicalPlanet.MERCURY
            Weekday.THURSDAY -> ClassicalPlanet.JUPITER
            Weekday.FRIDAY -> ClassicalPlanet.VENUS
            Weekday.SATURDAY -> ClassicalPlanet.SATURN
        }

    /** Rulers of the 24 hours of [weekday], in order. */
    public fun rulers(weekday: Weekday): List<ClassicalPlanet> {
        val first = rulerOfDay(weekday).ordinal
        return List(HOURS_PER_DAY) { ClassicalPlanet.entries[(first + it) % ClassicalPlanet.entries.size] }
    }

    /** The 24 hours of [weekday] given its [sunrise], [sunset] and the [nextSunrise]. */
    public fun hours(
        weekday: Weekday,
        sunrise: Instant,
        sunset: Instant,
        nextSunrise: Instant,
    ): List<PlanetaryHour> {
        require(sunrise < sunset && sunset < nextSunrise) { "expected sunrise < sunset < next sunrise" }
        val rulers = rulers(weekday)
        val dayHour = (sunset - sunrise) / HOURS_PER_HALF
        val nightHour = (nextSunrise - sunset) / HOURS_PER_HALF
        return List(HOURS_PER_DAY) { index ->
            val daytime = index < HOURS_PER_HALF
            val start = if (daytime) sunrise + dayHour * index else sunset + nightHour * (index - HOURS_PER_HALF)
            val end =
                when (index) {
                    HOURS_PER_HALF - 1 -> sunset
                    HOURS_PER_DAY - 1 -> nextSunrise
                    else -> start + if (daytime) dayHour else nightHour
                }
            PlanetaryHour(index + 1, start, end, rulers[index], daytime)
        }
    }

    /**
     * Planetary hours for [observer] of the day whose first sunrise follows [from], which falls on [weekday].
     * Unavailable when the Sun does not both rise and set, and at the edges of polar days and nights, where it only
     * grazes the horizon: a rise-set search that starts on such an event finds that same instant again, which would
     * leave a daytime or night shorter than [MIN_PERIOD].
     */
    public fun forDay(
        observer: Coordinates,
        from: Instant,
        weekday: Weekday,
    ): PlanetaryHoursResult {
        val sunrise = Sky.riseSetTransit(CelestialBody.SUN, observer, from).rise
        val sunset = sunrise?.let { Sky.riseSetTransit(CelestialBody.SUN, observer, it).set }
        val nextSunrise = sunset?.let { Sky.riseSetTransit(CelestialBody.SUN, observer, it).rise }
        val separated =
            sunrise != null && sunset != null && nextSunrise != null &&
                sunset - sunrise >= MIN_PERIOD && nextSunrise - sunset >= MIN_PERIOD
        return if (separated) {
            PlanetaryHoursResult.Available(hours(weekday, sunrise, sunset, nextSunrise))
        } else {
            PlanetaryHoursResult.Unavailable
        }
    }
}
