/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** A-13 façade: seasons golden plus invariants of Moon phases, rise/set and positions. */
class SkyTest {
    private val tehran = Coordinates(35.69, 51.39)

    @Test
    fun `March equinoxes match the official Iranian calendars within five minutes`() {
        GoldenFile
            .load("golden/iran/official-equinox-instants.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .forEach { (year, instant) ->
                val difference = Sky.seasons(year.toInt()).marchEquinox - Instant.parse(instant)
                difference.absoluteValue shouldBeLessThan 5.minutes
            }
    }

    @Test
    fun `equinoxes and solstices of 1700 to 2100 match USNO`() {
        val rows =
            GoldenFile
                .load("golden/usno/seasons-1700-2100.csv")
                .lines
                .drop(1)
                .map { it.split(',') }
        rows.size shouldBe 1604
        rows.groupBy { it[0].toInt() }.forEach { (year, events) ->
            val seasons = Sky.seasons(year)
            events.forEach { (_, event, instant) ->
                val deviation = (seasons.of(Season.valueOf(event.uppercase())) - Instant.parse(instant)).absoluteValue
                if (year in 2020..2040) deviation shouldBeLessThan 5.minutes
                deviation shouldBeLessThan usnoSeasonTolerance(year)
            }
        }
    }

    /**
     * PLAN §T-403 asks for ±5 min over 2020–2040; the wider USNO range gets bounds from the deviations measured on
     * 2026-09-15 with margin. USNO rounds to the minute (up to 30 s), and USNO and cosinekitty use different ΔT
     * (TT − UT) models, which diverge in extrapolation after about 2050. Largest |deviation|: 1700s 74 s, 1800s 62 s,
     * 1900s 62 s, 2000s 140 s (2020–2040: 80 s), 2100 112 s.
     */
    private fun usnoSeasonTolerance(year: Int) = if (year < 2000) 2.minutes else 3.minutes

    @Test
    fun `seasons of 2020 to 2040 are ordered and a tropical year apart`() {
        (2020..2040).zipWithNext().forEach { (year, next) ->
            val seasons = Sky.seasons(year)
            val instants = Season.entries.map(seasons::of)
            instants shouldBe instants.sorted()
            val yearLength = (Sky.seasons(next).marchEquinox - seasons.marchEquinox).inWholeMinutes / 1440.0
            yearLength shouldBe (365.2422 plusOrMinus 0.02)
            abs(Sky.eclipticPosition(CelestialBody.SUN, seasons.juneSolstice).longitudeDegrees - 90.0) shouldBe
                (0.0 plusOrMinus 0.01)
        }
    }

    @Test
    fun `Moon quarters cycle in order about 29,53 days apart`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val quarters = Sky.moonQuarters(start, start + 120.days)

        quarters.zipWithNext().forEach { (a, b) ->
            b.quarter.ordinal shouldBe (a.quarter.ordinal + 1) % 4
            a.instant shouldBeLessThan b.instant
        }
        quarters.filter { it.quarter == MoonQuarter.NEW_MOON }.zipWithNext().forEach { (a, b) ->
            (b.instant - a.instant).inWholeMinutes / 1440.0 shouldBe (29.53 plusOrMinus 0.6)
        }
        quarters.forEach { event ->
            val expected = event.quarter.ordinal * 90.0
            val phase = Sky.moonPhaseDegrees(event.instant)
            minOf(abs(phase - expected), 360 - abs(phase - expected)) shouldBe (0.0 plusOrMinus 0.01)
        }
        shouldThrow<IllegalArgumentException> { Sky.moonQuarters(start, start) }
    }

    @Test
    fun `the Moon image is mirrored in the southern hemisphere`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val firstQuarter = Sky.moonQuarters(start, start + 40.days).first { it.quarter == MoonQuarter.FIRST_QUARTER }
        val thirdQuarter = Sky.moonQuarters(start, start + 40.days).first { it.quarter == MoonQuarter.THIRD_QUARTER }
        val sydney = Coordinates(-33.87, 151.21)

        Sky.moonAppearance(firstQuarter.instant + 1.hours, tehran).brightLimbOnRight shouldBe true
        Sky.moonAppearance(firstQuarter.instant + 1.hours, sydney).brightLimbOnRight shouldBe false
        Sky.moonAppearance(thirdQuarter.instant + 1.hours, tehran).brightLimbOnRight shouldBe false
        Sky.moonAppearance(thirdQuarter.instant + 1.hours, sydney).brightLimbOnRight shouldBe true
        Sky.moonAppearance(firstQuarter.instant + 1.hours, tehran).waxing shouldBe true
        Sky.moonAppearance(firstQuarter.instant, tehran).illuminatedFraction shouldBe (0.5 plusOrMinus 0.02)
    }

    @Test
    fun `the Sun rises, transits and sets in order, except in polar night`() {
        val midnightTehran = Instant.parse("2026-09-12T20:30:00Z")
        val day = Sky.riseSetTransit(CelestialBody.SUN, tehran, midnightTehran)

        val rise = day.rise.shouldNotBeNull()
        val transit = day.transit.shouldNotBeNull()
        rise shouldBeLessThan transit
        transit shouldBeLessThan day.set.shouldNotBeNull()
        Sky.skyPosition(CelestialBody.SUN, transit, tehran).altitudeDegrees shouldBeGreaterThan 50.0

        val polarNight =
            Sky.riseSetTransit(
                CelestialBody.SUN,
                Coordinates(78.22, 15.65),
                Instant.parse("2026-12-21T00:00:00Z"),
            )
        polarNight.rise.shouldBeNull()
        polarNight.set.shouldBeNull()
        val moonTransit = Sky.riseSetTransit(CelestialBody.MOON, tehran, midnightTehran).transit.shouldNotBeNull()
        // The next lunar transit is about 24 h 50 min later, outside the one-day window.
        Sky.riseSetTransit(CelestialBody.MOON, tehran, moonTransit + 1.minutes).transit.shouldBeNull()
    }

    @Test
    fun `positions and libration stay within physical bounds`() {
        val instant = Instant.parse("2026-09-13T12:00:00Z")

        abs(Sky.eclipticPosition(CelestialBody.SUN, instant).latitudeDegrees) shouldBe (0.0 plusOrMinus 0.01)
        abs(Sky.eclipticPosition(CelestialBody.MOON, instant).latitudeDegrees) shouldBeLessThan 5.4
        val moon = Sky.skyPosition(CelestialBody.MOON, instant, tehran)
        moon.azimuthDegrees shouldBe (180.0 plusOrMinus 180.0)
        abs(moon.declinationDegrees) shouldBeLessThan 29.0
        val libration = Sky.libration(instant)
        abs(libration.latitudeDegrees) shouldBeLessThan 7.0
        abs(libration.longitudeDegrees) shouldBeLessThan 8.5
        libration.distanceKm shouldBe (384_400.0 plusOrMinus 25_000.0)
        libration.diameterDegrees shouldBe (0.52 plusOrMinus 0.05)
    }
}
