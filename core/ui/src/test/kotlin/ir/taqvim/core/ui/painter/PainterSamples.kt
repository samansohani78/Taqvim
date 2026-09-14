/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.WeekNumberModel
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.core.ui.theme.colorSchemeFor
import ir.taqvim.core.ui.theme.resolve
import kotlin.math.abs

/** Synthetic models of the painter tests; dates follow the calendars, events and holidays are made up. */
object PainterSamples {
    private val EVENT_COLORS = listOf(Color(0xFF3F8F5A), Color(0xFFB3261E), Color(0xFF6750A4), Color(0xFF7D5260))
    private const val PERSIAN_ZERO = '۰'
    private const val CHANNEL_TOLERANCE = 8

    /** The generated app palette (dynamic color off), light or [dark]. */
    fun palette(
        dark: Boolean,
        opaque: Boolean = true,
    ): PainterPalette {
        val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
        val appearance = settings.resolve(systemDark = dark, supportsDynamicColor = false)
        return PainterPalette.of(colorSchemeFor(appearance, settings.seedColor, null), opaque)
    }

    private fun persian(number: Int): String =
        Numerals.localizeDigits("$number", NumeralSystem.entries.first { it.zero == PERSIAN_ZERO })

    /** Farvardin 1405, which begins on Saturday 2026-03-21, in a Saturday-first week with Gregorian days below. */
    fun farvardin1405(): MonthGridModel {
        val isoNames =
            FormatTable.formats
                .getValue("fa")
                .weekdays
                .getValue(CalendarSystem.PERSIAN)
        val saturdayFirst = (isoNames.drop(5) + isoNames.take(5)).map { it.take(1) }
        val cells =
            List(35) { index ->
                val day = index + 1
                DayCellModel(
                    dayLabel = persian(if (day <= 31) day else day - 31),
                    contentDescription = "Farvardin $day",
                    secondaryLabels = listOf(persian((20 + index) % 31 + 1)),
                    indicators = List(index % 5) { EVENT_COLORS[it % EVENT_COLORS.size] },
                    isToday = day == 25,
                    isSelected = day == 26,
                    isHoliday = index % 7 == 6 || day in setOf(1, 2, 3, 4, 12, 13),
                    inCurrentMonth = day <= 31,
                )
            }
        return MonthGridModel(saturdayFirst, cells)
    }

    /** March 2026 (1 March is a Sunday) in a Monday-first week with week numbers. */
    fun march2026(): MonthGridModel {
        val names =
            FormatTable.formats
                .getValue("en")
                .weekdays
                .getValue(CalendarSystem.GREGORIAN)
                .map { it.take(3) }
        val cells =
            List(42) { index ->
                val day = index - 5
                val label =
                    when {
                        day < 1 -> 23 + index
                        day > 31 -> day - 31
                        else -> day
                    }
                DayCellModel(
                    dayLabel = "$label",
                    contentDescription = "Day $index",
                    indicators = if (day % 6 == 0) listOf(EVENT_COLORS[0]) else emptyList(),
                    isToday = day == 14,
                    isSelected = day == 20,
                    isHoliday = index % 7 >= 5,
                    inCurrentMonth = day in 1..31,
                )
            }
        val weeks = List(6) { WeekNumberModel("${it + 9}", "Week ${it + 9}") }
        return MonthGridModel(names, cells, weeks)
    }

    /** A made-up country, a coastline and a marker. */
    fun map(): MapThumbnailModel {
        fun point(
            x: Double,
            y: Double,
        ) = NormalizedPoint(x.toFloat(), y.toFloat())
        val country =
            listOf(
                point(0.1, 0.3),
                point(0.45, 0.1),
                point(0.85, 0.25),
                point(0.9, 0.7),
                point(0.5, 0.9),
                point(0.15, 0.75),
            )
        val coast = listOf(point(0.0, 0.95), point(0.3, 0.85), point(0.6, 0.98), point(1.0, 0.9))
        val shapes = listOf(MapShape(country, closed = true), MapShape(coast, closed = false))
        return MapThumbnailModel(shapes, point(0.55, 0.45))
    }

    /** Whether two colors match within a small per-channel tolerance. */
    fun near(
        a: Int,
        b: Int,
    ): Boolean = (0..3).all { abs((a shr (it * 8) and 0xFF) - (b shr (it * 8) and 0xFF)) <= CHANNEL_TOLERANCE }

    /** Pixels of this bitmap within [columns] that match [color]. */
    fun Bitmap.countNear(
        color: Int,
        columns: IntRange = 0 until width,
    ): Int {
        val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
        return pixels.indices.count { it % width in columns && near(pixels[it], color) }
    }
}
