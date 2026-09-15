/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1301 line layers: time-zone and plate boundaries on the flat map and behind the globe's horizon. */
class MapLineLayersTest {
    private val outline = MapFixtures.worldOutline()
    private val view = ViewSize(1_000f, 800f)

    @Test
    fun `every line point lies on the flat map at zoom 1`() {
        val viewport = MapViewport().clamped(view)
        val top = viewport.toScreen(MapPoint(0.0, 0.0), view)
        val bottom = viewport.toScreen(MapPoint(1.0, 1.0), view)
        (outline.timeZones + outline.plates).forEach { line ->
            for (index in 0 until line.size / 2) {
                val screen =
                    viewport.toScreen(
                        MapPoint(line[2 * index].toDouble(), line[2 * index + 1].toDouble()),
                        view,
                    )
                screen.x shouldBeGreaterThanOrEqual top.x - EPSILON
                screen.x shouldBeLessThanOrEqual bottom.x + EPSILON
                screen.y shouldBeGreaterThanOrEqual top.y - EPSILON
                screen.y shouldBeLessThanOrEqual bottom.y + EPSILON
            }
        }
    }

    @Test
    fun `the globe hides exactly the line points on its far side`(): Unit =
        runBlocking {
            val latitudes = Arb.double(-80.0, 80.0, includeNaNs = false)
            val longitudes = Arb.double(-180.0, 180.0, includeNaNs = false)
            val lines = outline.plates.take(SAMPLE_LINES) + outline.timeZones.take(SAMPLE_LINES)
            checkAll(PropertyTesting.iterations / 10, latitudes, longitudes) { latitude, longitude ->
                val globe = GlobeView(latitude, longitude)
                val frame = GlobeFrame(globe, view)
                lines.forEach { line ->
                    frame.polyline(line).forEachIndexed { index, screen ->
                        val place =
                            Equirectangular.unproject(
                                MapPoint(line[2 * index].toDouble(), line[2 * index + 1].toDouble()),
                            )
                        (screen != null) shouldBe globe.projection.project(place).visible
                    }
                }
            }
        }

    @Test
    fun `Iran's time-zone band and the Arabia-Eurasia plate boundary are drawn near Tehran`() {
        near(outline.timeZones, IRAN_WEST_LONGITUDES, IRAN_LATITUDES) shouldBeGreaterThan 0
        near(outline.plates, ZAGROS_LONGITUDES, ZAGROS_LATITUDES) shouldBeGreaterThan 0
    }

    /** How many points of [lines] fall inside the longitude and latitude ranges. */
    private fun near(
        lines: List<FloatArray>,
        longitudes: ClosedFloatingPointRange<Double>,
        latitudes: ClosedFloatingPointRange<Double>,
    ): Int =
        lines.sumOf { line ->
            (0 until line.size / 2).count { index ->
                val place =
                    Equirectangular.unproject(
                        MapPoint(line[2 * index].toDouble(), line[2 * index + 1].toDouble()),
                    )
                place.longitude in longitudes && place.latitude in latitudes
            }
        }

    private companion object {
        const val EPSILON = 0.5f
        const val SAMPLE_LINES = 40

        /** Iran's western border with Iraq and Turkey, where the +3:30 band meets +3. */
        val IRAN_WEST_LONGITUDES = 44.0..48.5
        val IRAN_LATITUDES = 30.0..39.5

        /** The Zagros belt, where the Arabian plate meets Eurasia. */
        val ZAGROS_LONGITUDES = 44.0..60.0
        val ZAGROS_LATITUDES = 25.0..38.0
    }
}
