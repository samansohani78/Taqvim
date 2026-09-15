/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** How the world is drawn: the flat plate carrée map or the globe (ADR-0024). */
enum class MapProjection {
    FLAT,
    GLOBE,
}

/**
 * A point of the orthographic projection of the unit sphere: [x] east and [y] north of the view's center; [visible]
 * when it lies on the near hemisphere.
 */
data class GlobePoint(
    val x: Double,
    val y: Double,
    val visible: Boolean,
)

/** The orthographic projection (the globe as seen from infinitely far away) centered on a place. */
class Orthographic(
    private val centerLatitude: Double,
    private val centerLongitude: Double,
) {
    private val sinCenter = sin(radians(centerLatitude))
    private val cosCenter = cos(radians(centerLatitude))

    fun project(place: Coordinates): GlobePoint = project(place.latitude, place.longitude)

    fun project(
        latitude: Double,
        longitude: Double,
    ): GlobePoint {
        val phi = radians(latitude)
        val lambda = radians(longitude - centerLongitude)
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val cosLambda = cos(lambda)
        return GlobePoint(
            x = cosPhi * sin(lambda),
            y = cosCenter * sinPhi - sinCenter * cosPhi * cosLambda,
            visible = sinCenter * sinPhi + cosCenter * cosPhi * cosLambda >= 0,
        )
    }

    /** The place of the near hemisphere at ([x], [y]), or `null` outside the unit disk. */
    fun unproject(
        x: Double,
        y: Double,
    ): Coordinates? {
        val rho = hypot(x, y)
        if (rho > 1) return null
        if (rho == 0.0) return Coordinates(centerLatitude, Equirectangular.wrapLongitude(centerLongitude))
        val c = asin(rho)
        val sinC = sin(c)
        val cosC = cos(c)
        val latitude = asin((cosC * sinCenter + y * sinC * cosCenter / rho).coerceIn(-1.0, 1.0))
        val longitude = atan2(x * sinC, rho * cosCenter * cosC - y * sinCenter * sinC)
        return Coordinates(degrees(latitude), Equirectangular.wrapLongitude(centerLongitude + degrees(longitude)))
    }

    private companion object {
        const val HALF_TURN = 180.0

        fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

        fun degrees(radians: Double): Double = radians * HALF_TURN / PI
    }
}

/** The globe view: the place at the view's center and the zoom; at [zoom] 1 the globe nearly fills the shorter side. */
@Immutable
data class GlobeView(
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val zoom: Double = 1.0,
) {
    val projection: Orthographic
        get() = Orthographic(centerLatitude, centerLongitude)

    /** The globe's radius in pixels. */
    fun radius(size: ViewSize): Float = min(size.width, size.height) / 2 * FILL * zoom.toFloat()

    /** Where [place] is drawn, or `null` when it is on the far side. */
    fun toScreen(
        place: Coordinates,
        size: ViewSize,
    ): ScreenPoint? = projection.project(place).takeIf { it.visible }?.let { screen(it, size) }

    /** [point] in pixels, also for points on the far side. */
    fun screen(
        point: GlobePoint,
        size: ViewSize,
    ): ScreenPoint {
        val radius = radius(size)
        return ScreenPoint(size.width / 2 + radius * point.x.toFloat(), size.height / 2 - radius * point.y.toFloat())
    }

    /** The place under [point], or `null` off the globe. */
    fun fromScreen(
        point: ScreenPoint,
        size: ViewSize,
    ): Coordinates? {
        if (size.isEmpty) return null
        val radius = radius(size).toDouble()
        return projection.unproject((point.x - size.width / 2) / radius, (size.height / 2 - point.y) / radius)
    }

    /** Turned so that the surface follows a drag of ([dx], [dy]) pixels; the center stays between the poles. */
    fun rotatedBy(
        dx: Float,
        dy: Float,
        size: ViewSize,
    ): GlobeView {
        if (size.isEmpty) return this
        val degreesPerPixel = HALF_TURN / (PI * radius(size))
        return copy(
            centerLatitude = (centerLatitude + dy * degreesPerPixel).coerceIn(-RIGHT_ANGLE, RIGHT_ANGLE),
            centerLongitude = Equirectangular.wrapLongitude(centerLongitude - dx * degreesPerPixel),
        )
    }

    /** Zoomed by [factor] within [MapViewport.MIN_ZOOM]‥[MapViewport.MAX_ZOOM]. */
    fun zoomedBy(factor: Double): GlobeView =
        if (!factor.isFinite() || factor <= 0) {
            this
        } else {
            copy(zoom = (zoom * factor).coerceIn(MapViewport.MIN_ZOOM, MapViewport.MAX_ZOOM))
        }

    private companion object {
        const val FILL = 0.94f
        const val HALF_TURN = 180.0
        const val RIGHT_ANGLE = 90.0
    }
}

/** The projection and both views; the one of [projection] is shown. */
@Immutable
internal data class MapCamera(
    val projection: MapProjection = MapProjection.FLAT,
    val viewport: MapViewport = MapViewport(),
    val globe: GlobeView = GlobeView(),
) {
    /** Where [place] is drawn, or `null` when it is on the globe's far side. */
    fun toScreen(
        place: Coordinates,
        size: ViewSize,
    ): ScreenPoint? =
        when (projection) {
            MapProjection.FLAT -> viewport.toScreen(Equirectangular.project(place), size)
            MapProjection.GLOBE -> globe.toScreen(place, size)
        }

    /** The place under [point]; on the flat map clamped to the map, on the globe `null` off the globe. */
    fun fromScreen(
        point: ScreenPoint,
        size: ViewSize,
    ): Coordinates? =
        when (projection) {
            MapProjection.FLAT -> Equirectangular.unproject(viewport.fromScreen(point, size).clampedToMap())
            MapProjection.GLOBE -> globe.fromScreen(point, size)
        }

    /** The place at the view's center. */
    fun center(): Coordinates =
        when (projection) {
            MapProjection.FLAT -> Equirectangular.unproject(MapPoint(viewport.centerX, viewport.centerY).clampedToMap())
            MapProjection.GLOBE -> Coordinates(globe.centerLatitude, globe.centerLongitude)
        }

    private fun MapPoint.clampedToMap(): MapPoint = MapPoint(x.coerceIn(0.0, 1.0), y.coerceIn(0.0, 1.0))
}
