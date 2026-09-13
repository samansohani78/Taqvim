/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.astronomy.EclipseKind
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-1300 "U header cache": what is computed once per hour and per day, eviction, and the real values. */
class AstronomyHeaderCacheTest {
    private val tehran = Coordinates(35.69, 51.39)
    private val start = Instant.parse("2026-06-01T10:05:00Z")

    private fun counting(capacity: Int = 4): Triple<AstronomyHeaderCache, MutableList<Instant>, MutableList<Instant>> {
        val moving = mutableListOf<Instant>()
        val daily = mutableListOf<Instant>()
        val cache =
            AstronomyHeaderCache(
                capacity = capacity,
                instantValues = { instant, _ ->
                    moving += instant
                    AstronomyHeaderCache.InstantValues(ZodiacSign.GEMINI, ZodiacSign.LEO, "Leo", 90.0, 0.5, true, 4e5)
                },
                dayValues = { instant ->
                    daily += instant
                    val seasons = listOf(Season.JUNE_SOLSTICE to instant + 20.days)
                    AstronomyHeaderCache.DayValues(seasons, listOf(), listOf())
                },
            )
        return Triple(cache, moving, daily)
    }

    @Test
    fun `values are computed once per hour and once per UTC day`() {
        val (cache, moving, daily) = counting()

        cache.header(start, tehran)
        cache.header(start + 50.minutes, tehran)
        cache.header(start + 54.minutes, tehran)
        cache.header(start + 3.hours, tehran)

        moving shouldBe listOf(Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T13:00:00Z"))
        daily shouldBe listOf(Instant.parse("2026-06-01T00:00:00Z"))
    }

    @Test
    fun `places are cached separately and old entries are evicted`() {
        val (cache, moving, daily) = counting(capacity = 2)

        cache.header(start, tehran)
        cache.header(start, Coordinates(-33.87, 151.21))
        cache.header(start, tehran)
        moving.size shouldBe 2

        cache.header(start + 1.hours, tehran)
        cache.header(start + 2.hours, tehran)
        cache.header(start, Coordinates(-33.87, 151.21))
        moving.size shouldBe 5

        cache.header(start + 2.days, tehran)
        cache.header(start + 3.days, tehran)
        cache.header(start, tehran)
        daily.size shouldBe 4
    }

    @Test
    fun `the default computation finds the next season and eclipses`() {
        val header = AstronomyHeaderCache().header(start, tehran)

        header.nextSeason shouldBe Season.JUNE_SOLSTICE
        (header.nextSeasonAt > start && header.nextSeasonAt < start + 21.days) shouldBe true
        // NASA GSFC eclipse catalogs: total solar eclipse 2026-08-12, partial lunar eclipse 2026-08-28.
        header.nextSolarEclipse?.kind shouldBe EclipseKind.TOTAL
        header.nextSolarEclipse
            ?.peak
            .toString()
            .take(10) shouldBe "2026-08-12"
        header.nextLunarEclipse?.kind shouldBe EclipseKind.PARTIAL
        header.nextLunarEclipse
            ?.peak
            .toString()
            .take(10) shouldBe "2026-08-28"
        header.sunSign shouldBe ZodiacSign.GEMINI
        (header.moonDistanceKm in 356_000.0..407_000.0) shouldBe true
        AstronomyHeaderCache
            .dayValues(Instant.parse("2026-12-25T00:00:00Z"))
            .seasons
            .first()
            .first shouldBe Season.MARCH_EQUINOX
    }

    @Test
    fun `the cache rejects a non-positive capacity`() {
        shouldThrow<IllegalArgumentException> { LruCache<String, String>(0) }
        LruCache<String, String>(1).apply {
            getOrPut("a") { "1" }
            getOrPut("b") { "2" }
            getOrPut("a") { "3" } shouldBe "3"
            size shouldBe 1
        }
    }
}
