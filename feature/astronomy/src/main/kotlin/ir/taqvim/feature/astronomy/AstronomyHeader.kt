/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Eclipses
import ir.taqvim.core.astronomy.GlobalSolarEclipse
import ir.taqvim.core.astronomy.LunarEclipse
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Zodiac
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Values of the screen header at an instant (T-1300): zodiac, Moon phase and distance, eclipses and seasons. */
data class AstronomyHeader(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    /** IAU abbreviation of the Moon's constellation, e.g. "Sco". */
    val moonConstellation: String,
    /** Moon's elongation east of the Sun, 0‥360°. */
    val moonPhaseDegrees: Double,
    val moonIlluminatedFraction: Double,
    val moonWaxing: Boolean,
    val moonDistanceKm: Double,
    val nextSeason: Season,
    val nextSeasonAt: Instant,
    val nextSolarEclipse: GlobalSolarEclipse?,
    val nextLunarEclipse: LunarEclipse?,
)

/**
 * Computes [AstronomyHeader]s and keeps the most recent ones (T-1300 "header cache"). Fast-changing values (Moon and
 * signs) are cached per whole hour, eclipse and season searches per UTC day; each cache holds [capacity] entries.
 */
class AstronomyHeaderCache(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val instantValues: (Instant, Coordinates) -> InstantValues = ::instantValues,
    private val dayValues: (Instant) -> DayValues = ::dayValues,
) {
    private val hourly = LruCache<Pair<Long, Coordinates>, InstantValues>(capacity)
    private val daily = LruCache<Long, DayValues>(capacity)

    /** Values changing within a day, computed at the start of their hour. */
    data class InstantValues(
        val sunSign: ZodiacSign,
        val moonSign: ZodiacSign,
        val moonConstellation: String,
        val moonPhaseDegrees: Double,
        val moonIlluminatedFraction: Double,
        val moonWaxing: Boolean,
        val moonDistanceKm: Double,
    )

    /**
     * Searches computed from the start of their UTC day: the following equinoxes and solstices and eclipses, enough of
     * each that the first one after any instant of that day is included.
     */
    data class DayValues(
        val seasons: List<Pair<Season, Instant>>,
        val solarEclipses: List<GlobalSolarEclipse>,
        val lunarEclipses: List<LunarEclipse>,
    )

    /** The header at [instant] for [place]. */
    fun header(
        instant: Instant,
        place: Coordinates,
    ): AstronomyHeader {
        val hour = Math.floorDiv(instant.toEpochMilliseconds(), MILLIS_PER_HOUR)
        val day = Math.floorDiv(instant.toEpochMilliseconds(), MILLIS_PER_DAY)
        val moving =
            hourly.getOrPut(hour to place) {
                instantValues(Instant.fromEpochMilliseconds(hour * MILLIS_PER_HOUR), place)
            }
        val slow = daily.getOrPut(day) { dayValues(Instant.fromEpochMilliseconds(day * MILLIS_PER_DAY)) }
        val (season, seasonAt) = slow.seasons.first { (_, at) -> at > instant }
        return AstronomyHeader(
            sunSign = moving.sunSign,
            moonSign = moving.moonSign,
            moonConstellation = moving.moonConstellation,
            moonPhaseDegrees = moving.moonPhaseDegrees,
            moonIlluminatedFraction = moving.moonIlluminatedFraction,
            moonWaxing = moving.moonWaxing,
            moonDistanceKm = moving.moonDistanceKm,
            nextSeason = season,
            nextSeasonAt = seasonAt,
            nextSolarEclipse = slow.solarEclipses.firstOrNull { it.peak > instant },
            nextLunarEclipse = slow.lunarEclipses.firstOrNull { it.peak > instant },
        )
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 48
        private const val MILLIS_PER_HOUR = 3_600_000L
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val ECLIPSE_SEARCH_DAYS = 800
        private const val ECLIPSES_KEPT = 2

        /** Moon and zodiac values at [instant] seen from [place]. */
        fun instantValues(
            instant: Instant,
            place: Coordinates,
        ): InstantValues {
            val appearance = Sky.moonAppearance(instant, place)
            return InstantValues(
                sunSign = Zodiac.tropicalSign(CelestialBody.SUN, instant),
                moonSign = Zodiac.tropicalSign(CelestialBody.MOON, instant),
                moonConstellation = Zodiac.moonConstellation(instant),
                moonPhaseDegrees = Sky.moonPhaseDegrees(instant),
                moonIlluminatedFraction = appearance.illuminatedFraction,
                moonWaxing = appearance.waxing,
                moonDistanceKm = Sky.libration(instant).distanceKm,
            )
        }

        /** The equinoxes, solstices and eclipses following [instant] (two of each eclipse kind). */
        fun dayValues(instant: Instant): DayValues {
            val year = instant.toLocalDateTime(TimeZone.UTC).year
            val seasons =
                (year..year + 1)
                    .flatMap { y -> Sky.seasons(y).let { seasons -> Season.entries.map { it to seasons.of(it) } } }
                    .filter { (_, at) -> at > instant }
            val until = instant + ECLIPSE_SEARCH_DAYS.days
            return DayValues(
                seasons = seasons,
                solarEclipses = Eclipses.solarEclipses(instant, until).take(ECLIPSES_KEPT),
                lunarEclipses = Eclipses.lunarEclipses(instant, until).take(ECLIPSES_KEPT),
            )
        }
    }
}

/** A bounded least-recently-used map; not shared between instances. */
internal class LruCache<K : Any, V : Any>(
    private val capacity: Int,
) {
    private val entries = LinkedHashMap<K, V>(capacity, LOAD_FACTOR, true)
    private val lock = Any()

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    val size: Int get() = synchronized(lock) { entries.size }

    fun getOrPut(
        key: K,
        compute: () -> V,
    ): V =
        synchronized(lock) {
            entries.getOrPut(key, compute).also {
                if (entries.size > capacity) entries.remove(entries.keys.first())
            }
        }

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}
