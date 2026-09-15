/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** ADR-0024 globe: orthographic projection round trips, the far side hidden, turning and zooming. */
class MapGlobeTest {
    private val view = ViewSize(900f, 1_200f)
    private val latitudes = Arb.double(-89.0, 89.0, includeNaNs = false)
    private val longitudes = Arb.double(-180.0, 180.0, includeNaNs = false)

    @Test
    fun `places on the near side project back to themselves`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, latitudes, longitudes, latitudes, longitudes) { lat0, lon0, lat, lon ->
                val projection = Orthographic(lat0, lon0)
                val point = projection.project(lat, lon)
                if (point.visible) {
                    hypot(point.x, point.y) shouldBeLessThanOrEqual 1.0 + 1e-12
                    val back = projection.unproject(point.x, point.y).shouldNotBeNull()
                    back.latitude shouldBe (lat plusOrMinus TOLERANCE)
                    if (abs(lat) < 89.0) longitudeGap(back.longitude, lon) shouldBeLessThanOrEqual TOLERANCE
                }
            }
        }

    @Test
    fun `points more than a quarter turn from the center are behind the horizon`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, latitudes, longitudes, latitudes, longitudes) { lat0, lon0, lat, lon ->
                val globe = GlobeView(lat0, lon0)
                val place = Coordinates(lat, lon)
                val angle = centralAngle(Coordinates(lat0, lon0), place)
                if (abs(angle - 90.0) > 1e-6) {
                    globe.projection.project(place).visible shouldBe (angle < 90.0)
                    (globe.toScreen(place, view) == null) shouldBe (angle > 90.0)
                }
                globe.toScreen(Coordinates(-lat0, Equirectangular.wrapLongitude(lon0 + 180)), view).shouldBeNull()
            }
        }

    @Test
    fun `screen positions on the globe pick the same place`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, latitudes, longitudes, latitudes, longitudes) { lat0, lon0, lat, lon ->
                val globe = GlobeView(lat0, lon0, zoom = 1.5)
                val place = Coordinates(lat, lon)
                val screen = globe.toScreen(place, view)
                // At the rim a pixel spans many degrees; float pixels are compared away from it.
                val nearRim = globe.projection.project(place).let { hypot(it.x, it.y) } > INNER_DISK
                if (screen != null && !nearRim) {
                    val back = globe.fromScreen(screen, view).shouldNotBeNull()
                    back.latitude shouldBe (lat plusOrMinus SCREEN_TOLERANCE)
                    if (abs(lat) < 85.0) longitudeGap(back.longitude, lon) shouldBeLessThanOrEqual SCREEN_TOLERANCE
                }
            }
        }

    @Test
    fun `the center is in the middle of the view and the rim is its edge`() {
        val globe = GlobeView(35.7, 51.42)
        globe.toScreen(Coordinates(35.7, 51.42), view) shouldBe ScreenPoint(450f, 600f)
        globe.fromScreen(ScreenPoint(450f, 600f), view) shouldBe Coordinates(35.7, 51.42)
        globe.fromScreen(ScreenPoint(0f, 0f), view).shouldBeNull()
        globe.fromScreen(ScreenPoint(1f, 1f), ViewSize(0f, 0f)).shouldBeNull()
        globe.radius(view) shouldBe 450f * 0.94f
        Orthographic(0.0, 0.0).unproject(0.0, 0.0) shouldBe Coordinates(0.0, 0.0)
    }

    @Test
    fun `dragging turns the globe with the finger and zoom stays in range`() {
        val globe = GlobeView(10.0, 20.0)
        val quarter = (PI * globe.radius(view) / 2).toFloat()

        globe.rotatedBy(quarter, 0f, view).centerLongitude shouldBe (-70.0 plusOrMinus 1e-3)
        globe.rotatedBy(0f, quarter, view).centerLatitude shouldBe 90.0
        globe.rotatedBy(0f, -quarter / 9, view).centerLatitude shouldBe (0.0 plusOrMinus 1e-3)
        globe.rotatedBy(quarter, quarter, ViewSize(0f, 0f)) shouldBe globe
        globe.zoomedBy(100.0).zoom shouldBe MapViewport.MAX_ZOOM
        globe.zoomedBy(0.01).zoom shouldBe MapViewport.MIN_ZOOM
        globe.zoomedBy(Double.NaN) shouldBe globe
    }

    @Test
    fun `the camera uses the shown projection`() {
        val flat = MapCamera()
        flat.center() shouldBe Coordinates(0.0, 0.0)
        flat.toScreen(Coordinates(0.0, 0.0), view) shouldBe ScreenPoint(450f, 600f)
        flat.fromScreen(ScreenPoint(-5_000f, 0f), view) shouldBe Coordinates(90.0, -180.0)

        val globe = MapCamera(MapProjection.GLOBE, globe = GlobeView(35.7, 51.42))
        globe.center() shouldBe Coordinates(35.7, 51.42)
        globe.toScreen(Coordinates(-35.7, -128.58), view).shouldBeNull()
        globe.fromScreen(ScreenPoint(450f, 600f), view) shouldBe Coordinates(35.7, 51.42)
    }

    private fun centralAngle(
        a: Coordinates,
        b: Coordinates,
    ): Double {
        val r = PI / 180
        val cosine =
            sin(a.latitude * r) * sin(b.latitude * r) +
                cos(a.latitude * r) * cos(b.latitude * r) * cos((b.longitude - a.longitude) * r)
        return acos(cosine.coerceIn(-1.0, 1.0)) / r
    }

    private fun longitudeGap(
        a: Double,
        b: Double,
    ): Double = abs(Equirectangular.wrapLongitude(a - b)).let { if (it > 180) 360 - it else it }

    private companion object {
        const val TOLERANCE = 1e-6

        /** Screen positions are floats: a pixel's thousandth is about 1e-4° on this globe. */
        const val SCREEN_TOLERANCE = 1e-2

        /** Radius, as a fraction of the globe's, inside which screen round trips are checked. */
        const val INNER_DISK = 0.99
    }
}
