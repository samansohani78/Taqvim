/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** A point of the world map in 0‥1 units: [x] runs from 180° W to 180° E, [y] from 90° N to 90° S. */
data class MapPoint(
    val x: Double,
    val y: Double,
)

/** A position in a view, in pixels from its top-left corner. */
data class ScreenPoint(
    val x: Float,
    val y: Float,
)

/** The size of the map view in pixels, and the pixels per dp ([density]) where the view reports it. */
data class ViewSize(
    val width: Float,
    val height: Float,
    val density: Float = 1f,
) {
    /** Whether the view has no area (not laid out yet). */
    val isEmpty: Boolean
        get() = width <= 0f || height <= 0f
}

/** The plate carrée (equirectangular) projection of the whole world onto a 2:1 map. */
object Equirectangular {
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0
    private const val RIGHT_ANGLE = 90.0

    fun project(coordinates: Coordinates): MapPoint = project(coordinates.latitude, coordinates.longitude)

    fun project(
        latitude: Double,
        longitude: Double,
    ): MapPoint = MapPoint((longitude + HALF_TURN) / FULL_TURN, (RIGHT_ANGLE - latitude) / HALF_TURN)

    /** The coordinates under [point]; latitude is clamped to ±90° and longitude wrapped into −180°‥180°. */
    fun unproject(point: MapPoint): Coordinates {
        val latitude = (RIGHT_ANGLE - point.y * HALF_TURN).coerceIn(-RIGHT_ANGLE, RIGHT_ANGLE)
        return Coordinates(latitude, wrapLongitude(point.x * FULL_TURN - HALF_TURN))
    }

    /** [longitude] wrapped into −180°‥180°; +180° itself is kept. */
    fun wrapLongitude(longitude: Double): Double {
        val wrapped = longitude - FULL_TURN * floor((longitude + HALF_TURN) / FULL_TURN)
        return if (wrapped == -HALF_TURN && longitude > 0) HALF_TURN else wrapped
    }
}

/**
 * The shown part of the map: at [zoom] 1 the map's width fills the view; ([centerX], [centerY]) is the map point at
 * the view's center.
 */
data class MapViewport(
    val zoom: Double = 1.0,
    val centerX: Double = HALF,
    val centerY: Double = HALF,
) {
    fun toScreen(
        point: MapPoint,
        size: ViewSize,
    ): ScreenPoint {
        val mapWidth = size.width * zoom
        return ScreenPoint(
            (size.width / 2 + (point.x - centerX) * mapWidth).toFloat(),
            (size.height / 2 + (point.y - centerY) * mapWidth / 2).toFloat(),
        )
    }

    fun fromScreen(
        point: ScreenPoint,
        size: ViewSize,
    ): MapPoint {
        val mapWidth = size.width * zoom
        return MapPoint(
            centerX + (point.x - size.width / 2) / mapWidth,
            centerY + (point.y - size.height / 2) / (mapWidth / 2),
        )
    }

    /** Zoomed by [factor] keeping the map point under [focus] in place, then [clamped]. */
    fun zoomedBy(
        factor: Double,
        focus: ScreenPoint,
        size: ViewSize,
    ): MapViewport {
        if (!factor.isFinite() || factor <= 0 || size.isEmpty) return this
        val anchor = fromScreen(focus, size)
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val mapWidth = size.width * newZoom
        return MapViewport(
            newZoom,
            anchor.x - (focus.x - size.width / 2) / mapWidth,
            anchor.y - (focus.y - size.height / 2) / (mapWidth / 2),
        ).clamped(size)
    }

    /** Moved so that the map follows a drag of ([dx], [dy]) pixels, then [clamped]. */
    fun pannedBy(
        dx: Float,
        dy: Float,
        size: ViewSize,
    ): MapViewport {
        if (size.isEmpty) return this
        val mapWidth = size.width * zoom
        return copy(centerX = centerX - dx / mapWidth, centerY = centerY - dy / (mapWidth / 2)).clamped(size)
    }

    /** Keeps the view inside the map along each axis where the map is larger than the view, centered otherwise. */
    fun clamped(size: ViewSize): MapViewport {
        if (size.isEmpty) return this
        val clampedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val mapWidth = size.width * clampedZoom
        return MapViewport(
            clampedZoom,
            centered(centerX, size.width / (2 * mapWidth)),
            centered(centerY, size.height / mapWidth),
        )
    }

    private fun centered(
        center: Double,
        halfView: Double,
    ): Double = if (halfView >= HALF || !center.isFinite()) HALF else center.coerceIn(halfView, 1 - halfView)

    companion object {
        const val MIN_ZOOM: Double = 1.0
        const val MAX_ZOOM: Double = 8.0
    }
}

private const val HALF = 0.5

/** Points along the shorter great circle between two places, and their split at the antimeridian for drawing. */
object GreatCirclePath {
    private const val DEFAULT_SEGMENTS = 128
    private const val EPSILON = 1e-9
    private const val HALF_TURN = 180.0

    /**
     * [segments] + 1 points from [from] to [to] by spherical linear interpolation; only the two ends when the places
     * coincide or are antipodal (no single great circle).
     */
    fun points(
        from: Coordinates,
        to: Coordinates,
        segments: Int = DEFAULT_SEGMENTS,
    ): List<Coordinates> {
        require(segments >= 1) { "segments must be positive (was $segments)" }
        val a = vector(from)
        val b = vector(to)
        val angle = acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0))
        val sinAngle = sin(angle)
        if (abs(sinAngle) < EPSILON) return listOf(from, to)
        return (0..segments).map { index ->
            when (index) {
                0 -> from
                segments -> to
                else -> interpolate(a, b, angle, sinAngle, index.toDouble() / segments)
            }
        }
    }

    /** [points] as map polylines, broken where a segment jumps more than 180° in longitude (the antimeridian). */
    fun polylines(points: List<Coordinates>): List<List<MapPoint>> =
        points.foldIndexed(emptyList<List<MapPoint>>()) { index, lines, point ->
            val breaks = index == 0 || abs(point.longitude - points[index - 1].longitude) > HALF_TURN
            val projected = Equirectangular.project(point)
            if (breaks) lines + listOf(listOf(projected)) else lines.dropLast(1) + listOf(lines.last() + projected)
        }

    private fun interpolate(
        a: DoubleArray,
        b: DoubleArray,
        angle: Double,
        sinAngle: Double,
        t: Double,
    ): Coordinates {
        val wa = sin((1 - t) * angle) / sinAngle
        val wb = sin(t * angle) / sinAngle
        val x = wa * a[0] + wb * b[0]
        val y = wa * a[1] + wb * b[1]
        val z = wa * a[2] + wb * b[2]
        return Coordinates(degrees(asin(z.coerceIn(-1.0, 1.0))), degrees(atan2(y, x)))
    }

    private fun vector(place: Coordinates): DoubleArray {
        val latitude = radians(place.latitude)
        val longitude = radians(place.longitude)
        return doubleArrayOf(cos(latitude) * cos(longitude), cos(latitude) * sin(longitude), sin(latitude))
    }

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
