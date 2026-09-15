/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import ir.taqvim.core.model.Coordinates
import kotlin.math.hypot

/** Which catalog cities get a marker: the most populous in view, fewer when zoomed out, never crowded together. */
object CityMarkers {
    /** Cities asked from [MapCitySource]: the whole bundled catalog. */
    const val CANDIDATES: Int = 10_000

    /** Least distance between two markers, in dp. */
    const val SPACING_DP: Float = 56f

    /** How far from a marker a tap still picks it, in dp. */
    const val TOUCH_DP: Float = 24f

    private const val BASE_COUNT = 12
    private const val MAX_COUNT = 120

    /** Most markers at [zoom]: 12 on the whole map, growing with the zoomed area (zoom²), at most 120. */
    fun maxCount(zoom: Double): Int = (BASE_COUNT * zoom * zoom).toInt().coerceIn(BASE_COUNT, MAX_COUNT)

    /**
     * Up to [limit] of [cities] (most populous first): each is taken in order when [place] puts it inside [size] and at
     * least [spacing] pixels from every city already taken. [place] is `null` where a city cannot be seen.
     */
    fun select(
        cities: List<MapCity>,
        size: ViewSize,
        spacing: Float,
        limit: Int,
        place: (Coordinates) -> ScreenPoint?,
    ): List<MapCity> {
        if (size.isEmpty || limit <= 0) return emptyList()
        val taken = ArrayList<MapCity>(limit)
        val positions = ArrayList<ScreenPoint>(limit)
        for (city in cities) {
            val position = place(city.coordinates)
            val fits =
                position != null &&
                    position.x in 0f..size.width &&
                    position.y in 0f..size.height &&
                    positions.none { distance(it, position) < spacing }
            if (fits) {
                taken += city
                positions += position
                if (taken.size == limit) break
            }
        }
        return taken
    }

    /** The city of [shown] nearest to [point] within [radius] pixels, if any. */
    fun hit(
        shown: List<MapCity>,
        point: ScreenPoint,
        radius: Float,
        place: (Coordinates) -> ScreenPoint?,
    ): MapCity? =
        shown
            .mapNotNull { city -> place(city.coordinates)?.let { city to distance(it, point) } }
            .filter { (_, distance) -> distance <= radius }
            .minByOrNull { (_, distance) -> distance }
            ?.first

    private fun distance(
        a: ScreenPoint,
        b: ScreenPoint,
    ): Float = hypot(a.x - b.x, a.y - b.y)
}
