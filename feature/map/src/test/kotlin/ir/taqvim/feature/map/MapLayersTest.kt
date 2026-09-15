/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Yallop
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import kotlin.math.abs
import kotlin.system.measureTimeMillis
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** T-1301 layers: subsolar and sublunar points against `:core:astronomy`, day/night mask, grids. */
class MapLayersTest {
    private val from = Instant.parse("2020-01-01T00:00:00Z").toEpochMilliseconds()
    private val until = Instant.parse("2030-01-01T00:00:00Z").toEpochMilliseconds()

    @Test
    fun `subsolar latitude at the 2026 equinoxes and solstices`() {
        val seasons = Sky.seasons(2026)
        SubPoints.sun(seasons.marchEquinox).latitude shouldBe (0.0 plusOrMinus 0.03)
        SubPoints.sun(seasons.septemberEquinox).latitude shouldBe (0.0 plusOrMinus 0.03)
        SubPoints.sun(seasons.juneSolstice).latitude shouldBe (23.436 plusOrMinus 0.03)
        SubPoints.sun(seasons.decemberSolstice).latitude shouldBe (-23.436 plusOrMinus 0.03)
    }

    @Test
    fun `the Sun and the Moon are overhead at their sub-points`(): Unit =
        runBlocking {
            checkAll(minOf(PropertyTesting.iterations, 200), Arb.long(from..until)) { millis ->
                val instant = Instant.fromEpochMilliseconds(millis)
                val sun = Sky.skyPosition(CelestialBody.SUN, instant, SubPoints.sun(instant))
                val moon = Sky.skyPosition(CelestialBody.MOON, instant, SubPoints.moon(instant))
                sun.altitudeDegrees shouldBeGreaterThan 89.5
                moon.altitudeDegrees shouldBeGreaterThan 88.5
            }
        }

    @Test
    fun `altitudes from the sub-points match the ephemeris`(): Unit =
        runBlocking {
            val iterations = minOf(PropertyTesting.iterations, 200)
            checkAll(iterations, Arb.long(from..until), Arb.int(-6_000..6_000), Arb.int(-17_900..17_900)) {
                millis,
                lat,
                lon,
                ->
                val instant = Instant.fromEpochMilliseconds(millis)
                val place = Coordinates(lat / 100.0, lon / 100.0)
                listOf(CelestialBody.SUN to 0.3, CelestialBody.MOON to 1.2).forEach { (body, tolerance) ->
                    val sub = if (body == CelestialBody.SUN) SubPoints.sun(instant) else SubPoints.moon(instant)
                    val computed = SubPoints.altitudeDegrees(sub, place.latitude, place.longitude)
                    if (computed >= 10) {
                        Sky.skyPosition(body, instant, place).altitudeDegrees shouldBe (computed plusOrMinus tolerance)
                    }
                }
            }
        }

    @Test
    fun `the terminator at the March equinox runs through the poles`() {
        val sun = SubPoints.sun(Sky.seasons(2026).marchEquinox)
        val grid = LayerGrids.illumination(sun)
        val column = ((sun.longitude + 180) / 2).toInt()
        grid[column, 45] shouldBe Illumination.DAY.ordinal
        grid[(column + 90) % 180, 45] shouldBe Illumination.NIGHT.ordinal
        val dusk = Equirectangular.wrapLongitude(sun.longitude + 90)
        SubPoints.altitudeDegrees(sun, 0.0, dusk) shouldBe (0.0 plusOrMinus 0.05)
        SubPoints.altitudeDegrees(sun, 89.9, sun.longitude + 45) shouldBe (0.0 plusOrMinus 0.2)
    }

    @Test
    fun `daylight classes follow the Sun's altitude`() {
        Illumination.of(10.0) shouldBe Illumination.DAY
        Illumination.of(-0.5) shouldBe Illumination.DAY
        Illumination.of(-1.0) shouldBe Illumination.CIVIL_TWILIGHT
        Illumination.of(-7.0) shouldBe Illumination.NAUTICAL_TWILIGHT
        Illumination.of(-13.0) shouldBe Illumination.ASTRONOMICAL_TWILIGHT
        Illumination.of(-19.0) shouldBe Illumination.NIGHT
    }

    @Test
    fun `the Moon is above the horizon over half the world`() {
        val grid = LayerGrids.moonVisibility(SubPoints.moon(MapFixtures.NOON))
        var up = 0
        for (row in 0 until grid.rows) for (column in 0 until grid.columns) up += grid[column, row]
        (up * 100 / (grid.rows * grid.columns)) shouldBeInRange 47..53
    }

    @Test
    fun `grids hold rounded declination and crescent classes where they exist`() {
        val declination = LayerGrids.declination(MapFixtures.MAGNETIC, MapFixtures.NOON)
        declination[0, 0] shouldBe ShadeGrid.NONE
        declination[71, 20] shouldBe 30
        declination[0, 20] shouldBe -30
        val crescent = LayerGrids.crescent(Instant.parse("2026-08-12T00:00:00Z"), columns = 4, rows = 2)
        for (row in 0 until 2) {
            for (column in 0 until 4) {
                val place = Coordinates(ShadeGrid.latitudeOf(row, 2), ShadeGrid.longitudeOf(column, 4))
                val expected = Yallop.evening(place, Instant.parse("2026-08-12T00:00:00Z"))?.visibility?.ordinal
                crescent[column, row] shouldBe (expected ?: ShadeGrid.NONE)
            }
        }
    }

    @Test
    fun `grids compare by content and reject empty sizes`() {
        val a = ShadeGrid.build(3, 2) { lat, lon -> (lat + lon).toInt() }
        a shouldBe ShadeGrid.build(3, 2) { lat, lon -> (lat + lon).toInt() }
        a.hashCode() shouldBe ShadeGrid.build(3, 2) { lat, lon -> (lat + lon).toInt() }.hashCode()
        a shouldNotBe ShadeGrid.build(3, 2) { _, _ -> 0 }
        ShadeGrid.latitudeOf(0, 90) shouldBe 89.0
        ShadeGrid.longitudeOf(0, 180) shouldBe -179.0
        shouldThrow<IllegalArgumentException> { ShadeGrid.build(0, 2) { _, _ -> 0 } }
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a one-degree day and night mask is computed within 150 ms`() {
        val sun = SubPoints.sun(MapFixtures.NOON)
        repeat(3) { LayerGrids.illumination(sun, 360, 180) }
        val best = (1..5).minOf { measureTimeMillis { LayerGrids.illumination(sun, 360, 180) } }
        best shouldBeLessThan 150L
        abs(sun.latitude) shouldBeGreaterThan 0.0
    }
}
