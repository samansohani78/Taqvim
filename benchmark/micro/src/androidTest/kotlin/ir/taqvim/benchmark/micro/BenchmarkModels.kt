/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark.micro

import android.content.Context
import androidx.compose.ui.graphics.Color
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.painter.MoonBitmapModel
import ir.taqvim.core.ui.painter.NormalizedPoint
import ir.taqvim.core.ui.painter.PainterPalette
import ir.taqvim.core.ui.painter.ProgressRingBitmapModel
import ir.taqvim.feature.map.WorldOutline
import ir.taqvim.feature.map.WorldOutlineParser
import kotlin.time.Instant

/** Synthetic but widget-sized painter inputs for the render benchmarks; dates and events are made up. */
internal object BenchmarkModels {
    /** The 2026 March equinox at noon UTC: day and night split the map evenly. */
    val EQUINOX: Instant = Instant.parse("2026-03-20T12:00:00Z")

    private const val WEEK = 7
    private const val GRID_CELLS = 42
    private const val FIRST_DAY_INDEX = 6
    private const val MONTH_DAYS = 31
    private const val EVENT_EVERY = 3
    private const val HOLIDAY_EVERY = 7
    private const val TODAY = 25
    private const val SUN_PROGRESS = 0.4f
    private const val MOON_FRACTION = 0.62f
    private const val RING_PROGRESS = 0.7f
    private const val MARKER_X = 0.64f
    private const val MARKER_Y = 0.30f
    private const val OPAQUE = 0xFF000000.toInt()

    /** A light widget palette as ARGB ints (the painters only read these colors). */
    val PALETTE: PainterPalette =
        PainterPalette(
            background = 0,
            surfaceHighest = OPAQUE or 0xE6E1E5,
            onSurface = OPAQUE or 0x1C1B1F,
            onSurfaceVariant = OPAQUE or 0x49454F,
            outline = OPAQUE or 0x79747E,
            outlineVariant = OPAQUE or 0xCAC4D0,
            primary = OPAQUE or 0x3F8F5A,
            onPrimary = OPAQUE or 0xFFFFFF,
            error = OPAQUE or 0xB3261E,
            tertiary = OPAQUE or 0x7D5260,
            land = OPAQUE or 0xD0E8D5,
        )

    private val EVENT_COLOR = Color(0xFF3F8F5A)

    /** A six-week month in Persian digits with secondary dates, holidays and event dots (the 4 × 4 widget). */
    fun month(): MonthGridModel {
        val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val cells =
            List(GRID_CELLS) { index ->
                val day = index - FIRST_DAY_INDEX + 1
                val shownDay = if (day in 1..MONTH_DAYS) day else (day + MONTH_DAYS - 1) % MONTH_DAYS + 1
                DayCellModel(
                    dayLabel = persianDigits(shownDay),
                    contentDescription = "Day $index",
                    secondaryLabels = listOf("${(index + TODAY) % MONTH_DAYS + 1}"),
                    indicators = if (index % EVENT_EVERY == 0) listOf(EVENT_COLOR) else emptyList(),
                    isToday = day == TODAY,
                    isHoliday = index % HOLIDAY_EVERY == WEEK - 1,
                    inCurrentMonth = day in 1..MONTH_DAYS,
                )
            }
        return MonthGridModel(weekdays, cells)
    }

    fun sun(): SunArcModel = SunArcModel(SUN_PROGRESS, "۶:۰۴", "۱۸:۱۲", "Sun")

    fun moon(): MoonBitmapModel = MoonBitmapModel(MOON_FRACTION, waxing = true)

    fun ring(): ProgressRingBitmapModel = ProgressRingBitmapModel(RING_PROGRESS)

    /** Tehran on the normalized map, as the map widget's place marker. */
    val MARKER: NormalizedPoint = NormalizedPoint(MARKER_X, MARKER_Y)

    /** The bundled Natural Earth outline of `:feature:map` (the map widget's land). */
    fun outline(context: Context): WorldOutline =
        WorldOutlineParser.parse(
            context.assets
                .open(WorldOutlineParser.ASSET)
                .bufferedReader()
                .use { it.readText() },
        )

    private fun persianDigits(number: Int): String = number.toString().map { '۰' + (it - '0') }.joinToString("")
}
