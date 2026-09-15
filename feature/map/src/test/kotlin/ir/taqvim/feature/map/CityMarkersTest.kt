/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.hypot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1301 city markers: zoom-dependent density, spacing, order and hits. */
class CityMarkersTest {
    private val view = ViewSize(1_000f, 800f)

    /** A dense synthetic catalog: one city every 3° with population falling from north-west to south-east. */
    private val grid: List<MapCity> =
        (-87..87 step 3)
            .flatMap { latitude -> (-180 until 180 step 3).map { longitude -> latitude to longitude } }
            .mapIndexed { index, (latitude, longitude) ->
                MapCity(index.toLong(), "c$index", Coordinates(-latitude.toDouble(), longitude.toDouble()), 1L + index)
            }.sortedByDescending { it.population }

    @Test
    fun `more markers are allowed as the map is zoomed in`() {
        CityMarkers.maxCount(1.0) shouldBe 12
        CityMarkers.maxCount(2.0) shouldBe 48
        CityMarkers.maxCount(8.0) shouldBe 120
        (1..16).map { CityMarkers.maxCount(it / 2.0) }.zipWithNext().all { (a, b) -> a <= b } shouldBe true
    }

    @Test
    fun `zooming in shows more of the catalog in a smaller area`() {
        fun count(viewport: MapViewport): Int =
            CityMarkers
                .select(grid, view, SPACING, CityMarkers.maxCount(viewport.zoom)) {
                    viewport.toScreen(Equirectangular.project(it), view)
                }.size

        val whole = count(MapViewport())
        val zoomed = count(MapViewport(zoom = 4.0, centerX = 0.6, centerY = 0.3))
        whole shouldBe 12
        zoomed shouldBeGreaterThan whole
        zoomed shouldBeLessThanOrEqual CityMarkers.maxCount(4.0)
    }

    @Test
    fun `selected markers are in view, spaced, in population order and leave out only crowded cities`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.list(Arb.double(-89.0, 89.0, includeNaNs = false), 0..60),
                Arb.double(1.0, 8.0, includeNaNs = false),
                Arb.double(0.0, 1.0, includeNaNs = false),
                Arb.int(1..40),
            ) { latitudes, zoom, centerX, limit ->
                val cities =
                    latitudes.mapIndexed { index, latitude ->
                        MapCity(index.toLong(), "c$index", Coordinates(latitude, index * 6.0 - 180), 1_000L - index)
                    }
                val viewport = MapViewport(zoom, centerX).clamped(view)
                val place = { coordinates: Coordinates ->
                    viewport.toScreen(Equirectangular.project(coordinates), view)
                }
                val selected = CityMarkers.select(cities, view, SPACING, limit, place)
                val positions = selected.map { place(it.coordinates) }

                selected.size shouldBeLessThanOrEqual limit
                positions.all { it.x in 0f..view.width && it.y in 0f..view.height } shouldBe true
                val spaced =
                    positions.withIndex().all { (a, first) ->
                        positions.withIndex().all { (b, second) -> a == b || gap(first, second) >= SPACING }
                    }
                spaced shouldBe true
                cities.filter { it in selected } shouldContainExactly selected
                if (selected.size < limit) {
                    cities.filter { it !in selected }.forEach { city ->
                        val position = place(city.coordinates)
                        val inView = position.x in 0f..view.width && position.y in 0f..view.height
                        (!inView || positions.any { gap(it, position) < SPACING }) shouldBe true
                    }
                }
            }
        }

    @Test
    fun `hidden cities and empty views get no markers`() {
        CityMarkers.select(grid, view, SPACING, 10) { null }.shouldBeEmpty()
        CityMarkers.select(grid, ViewSize(0f, 0f), SPACING, 10) { ScreenPoint(0f, 0f) }.shouldBeEmpty()
        CityMarkers.select(grid, view, SPACING, 0) { ScreenPoint(1f, 1f) }.shouldBeEmpty()
    }

    @Test
    fun `a tap picks the nearest shown city within its radius`() {
        val cities = MapFixtures.cities()
        val tehran = cities.first { it.name == "Tehran" }
        val karaj = cities.first { it.name == "Karaj" }
        val positions = mapOf(tehran.id to ScreenPoint(100f, 100f), karaj.id to ScreenPoint(130f, 100f))
        val place = { coordinates: Coordinates ->
            cities.first { it.coordinates == coordinates }.let { positions[it.id] }
        }

        CityMarkers.hit(cities, ScreenPoint(110f, 104f), 24f, place) shouldBe tehran
        CityMarkers.hit(cities, ScreenPoint(122f, 96f), 24f, place) shouldBe karaj
        CityMarkers.hit(cities, ScreenPoint(200f, 200f), 24f, place).shouldBeNull()
    }

    private fun gap(
        a: ScreenPoint,
        b: ScreenPoint,
    ): Float = hypot(a.x - b.x, a.y - b.y)

    private companion object {
        const val SPACING = 56f
    }
}
