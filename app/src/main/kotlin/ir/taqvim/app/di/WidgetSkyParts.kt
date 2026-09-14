/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.painter.NormalizedPoint
import ir.taqvim.feature.map.Equirectangular
import ir.taqvim.feature.map.Illumination
import ir.taqvim.feature.map.LayerGrids
import ir.taqvim.feature.map.SubPoints
import ir.taqvim.feature.map.WorldOutlineSource
import ir.taqvim.feature.widgets.WidgetCalendarBuilder
import ir.taqvim.feature.widgets.WidgetData
import ir.taqvim.feature.widgets.WidgetDayInputs
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetMap
import ir.taqvim.feature.widgets.WidgetSkyBuilder
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone

/**
 * The Sun, Moon and map widgets' parts (T-1209…T-1211). Features cannot depend on each other, so the world map's
 * geometry stays in `:feature:map` (the Natural Earth outline, the subsolar point, the twilight grid and the
 * equirectangular projection) and reaches `:feature:widgets` here as plain normalized points and darkness values. The
 * parsed outline is loaded once and kept for the process.
 */
internal class WidgetSkyParts(
    private val outline: WorldOutlineSource,
) {
    private val outlineLock = Mutex()
    private var land: List<FloatArray>? = null

    /** [data] with the part of [kind] for [inputs] at [now] in [zone]; other kinds are returned unchanged. */
    suspend fun addTo(
        data: WidgetData,
        kind: WidgetKind,
        inputs: WidgetDayInputs,
        now: Instant,
        zone: TimeZone,
    ): WidgetData {
        val place = inputs.place
        return when (kind) {
            WidgetKind.SUN_ARC -> {
                val sun = place?.let { WidgetCalendarBuilder.sun(now, it, inputs.language) }
                data.copy(sun = sun, daylightUnavailable = place != null && sun == null)
            }

            WidgetKind.MOON -> {
                data.copy(moon = WidgetSkyBuilder.moon(now, place?.coordinates, inputs.primary, zone, inputs.language))
            }

            WidgetKind.MAP -> {
                data.copy(map = map(now, place?.coordinates))
            }

            else -> {
                data
            }
        }
    }

    private suspend fun map(
        now: Instant,
        place: Coordinates?,
    ): WidgetMap {
        val grid = LayerGrids.illumination(SubPoints.sun(now), SHADE_COLUMNS, SHADE_ROWS)
        val darkest = (Illumination.entries.size - 1).toFloat()
        val marker = place?.let { Equirectangular.project(it) }?.let { NormalizedPoint(it.x.toFloat(), it.y.toFloat()) }
        return WidgetSkyBuilder.map(
            land(),
            grid.columns,
            grid.rows,
            { column, row -> grid[column, row] / darkest },
            marker,
        )
    }

    /** The land rings, loaded once; an unreadable outline leaves only day and night and is retried next time. */
    private suspend fun land(): List<FloatArray> =
        outlineLock.withLock {
            land ?: run {
                val result = runCatching { outline.load().land }
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                result.getOrNull()?.also { land = it }.orEmpty()
            }
        }

    private companion object {
        /** A 5° grid: fine enough for a 4 × 2 widget, cheap enough to draw every minute. */
        const val SHADE_COLUMNS = 72
        const val SHADE_ROWS = 36
    }
}
