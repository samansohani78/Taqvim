/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.astronomy.GreatCircle
import ir.taqvim.core.astronomy.Qibla
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1301 geometry: projection, viewport and great-circle paths. */
class MapGeometryTest {
    private val view = ViewSize(1_000f, 800f)

    /** How many neighbouring doubles on each side of a half turn are checked. */
    private val ulpSteps = 40

    @Test
    fun `projection round trips every coordinate`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-9_000..9_000), Arb.int(-17_999..17_999)) { lat, lon ->
                val place = Coordinates(lat / 100.0, lon / 100.0)
                val back = Equirectangular.unproject(Equirectangular.project(place))
                back.latitude shouldBe (place.latitude plusOrMinus 1e-9)
                back.longitude shouldBe (place.longitude plusOrMinus 1e-9)
            }
        }

    @Test
    fun `longitudes wrap into the map`() {
        Equirectangular.wrapLongitude(190.0) shouldBe (-170.0 plusOrMinus 1e-9)
        Equirectangular.wrapLongitude(-190.0) shouldBe (170.0 plusOrMinus 1e-9)
        Equirectangular.wrapLongitude(180.0) shouldBe 180.0
        Equirectangular.wrapLongitude(-180.0) shouldBe -180.0
        Equirectangular.wrapLongitude(540.0) shouldBe 180.0
        // Rounding regression: this input used to wrap to −180.00000000000003 and fail Coordinates validation.
        Equirectangular.wrapLongitude(179.99999999999997) shouldBe (180.0 plusOrMinus 1e-9)
        Equirectangular.wrapLongitude(899.9999999999999) shouldBe (180.0 plusOrMinus 1e-9)
    }

    @Test
    fun `longitudes next to every half turn stay inside the map`() {
        (-20..20).forEach { turn ->
            listOf<(Double) -> Double>({ it.nextUp() }, { it.nextDown() }).forEach { step ->
                var longitude = 180.0 * (2 * turn + 1)
                repeat(ulpSteps) {
                    longitude = step(longitude)
                    val wrapped = Equirectangular.wrapLongitude(longitude)
                    wrapped shouldBeGreaterThanOrEqual -180.0
                    wrapped shouldBeLessThanOrEqual 180.0
                    Coordinates(0.0, wrapped).longitude shouldBe wrapped
                }
            }
        }
        Equirectangular.project(Coordinates(90.0, -180.0)) shouldBe MapPoint(0.0, 0.0)
        Equirectangular.unproject(MapPoint(0.5, 2.0)) shouldBe Coordinates(-90.0, 0.0)
    }

    @Test
    fun `screen positions round trip and the view stays on the map`(): Unit =
        runBlocking {
            val pans = Arb.int(-3_000..3_000)
            checkAll(PropertyTesting.iterations, Arb.int(0..700), pans, pans) { z, dx, dy ->
                val zoomed = MapViewport().zoomedBy(1 + z / 100.0, ScreenPoint(300f, 200f), view)
                val viewport = zoomed.pannedBy(dx.toFloat(), dy.toFloat(), view)
                val point = MapPoint(0.25, 0.75)
                val back = viewport.fromScreen(viewport.toScreen(point, view), view)
                back.x shouldBe (point.x plusOrMinus 1e-5)
                back.y shouldBe (point.y plusOrMinus 1e-5)
                viewport.zoom shouldBeGreaterThanOrEqual MapViewport.MIN_ZOOM
                viewport.zoom shouldBeLessThanOrEqual MapViewport.MAX_ZOOM
                viewport.toScreen(MapPoint(0.0, 0.0), view).x shouldBeLessThanOrEqual 0.5f
                viewport.toScreen(MapPoint(1.0, 0.0), view).x shouldBeGreaterThanOrEqual view.width - 0.5f
            }
        }

    @Test
    fun `zooming keeps the point under the fingers`() {
        val zoomed = MapViewport().zoomedBy(4.0, ScreenPoint(500f, 400f), view)
        zoomed.zoom shouldBe 4.0
        zoomed.fromScreen(ScreenPoint(500f, 400f), view).x shouldBe (0.5 plusOrMinus 1e-9)
        val inside = zoomed.zoomedBy(2.0, ScreenPoint(600f, 300f), view)
        val before = zoomed.fromScreen(ScreenPoint(600f, 300f), view)
        val after = inside.fromScreen(ScreenPoint(600f, 300f), view)
        after.x shouldBe (before.x plusOrMinus 1e-9)
        after.y shouldBe (before.y plusOrMinus 1e-9)
        MapViewport().zoomedBy(100.0, ScreenPoint(0f, 0f), view).zoom shouldBe MapViewport.MAX_ZOOM
        MapViewport().zoomedBy(Double.NaN, ScreenPoint(0f, 0f), view) shouldBe MapViewport()
        MapViewport(zoom = 3.0).clamped(ViewSize(0f, 0f)) shouldBe MapViewport(zoom = 3.0)
    }

    @Test
    fun `great circle to the Kaaba lies on the circle and ends there`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-8_000..8_000), Arb.int(-17_900..17_900)) { lat, lon ->
                val from = Coordinates(lat / 100.0, lon / 100.0)
                val total = GreatCircle.centralAngleRadians(from, Qibla.KAABA)
                val points = GreatCirclePath.points(from, Qibla.KAABA, segments = 32)
                if (total > 1e-6 && abs(total - Math.PI) > 1e-6) {
                    points.first() shouldBe from
                    points.last() shouldBe Qibla.KAABA
                    points.size shouldBe 33
                    points.forEach { point ->
                        val first = GreatCircle.centralAngleRadians(from, point)
                        (first + GreatCircle.centralAngleRadians(point, Qibla.KAABA)) shouldBe (total plusOrMinus 1e-6)
                    }
                }
            }
        }

    @Test
    fun `paths break at the antimeridian`() {
        val tokyo = Coordinates(35.68, 139.69)
        val sanFrancisco = Coordinates(37.77, -122.42)
        val lines = GreatCirclePath.polylines(GreatCirclePath.points(tokyo, sanFrancisco))
        lines.size shouldBe 2
        lines.forEach { line -> line.zipWithNext().forEach { (a, b) -> abs(a.x - b.x) shouldBeLessThan 0.5 } }
        GreatCirclePath.points(tokyo, tokyo) shouldBe listOf(tokyo, tokyo)
        GreatCirclePath.points(Coordinates(0.0, 0.0), Coordinates(0.0, 180.0)).size shouldBe 2
        GreatCirclePath.polylines(emptyList()) shouldBe emptyList()
        shouldThrow<IllegalArgumentException> { GreatCirclePath.points(tokyo, Qibla.KAABA, segments = 0) }
    }
}
