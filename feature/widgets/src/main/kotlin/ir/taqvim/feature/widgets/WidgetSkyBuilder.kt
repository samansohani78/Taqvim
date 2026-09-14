/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.astronomy.MoonQuarter
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.painter.NormalizedPoint
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone

/**
 * The Moon for the Moon widget (T-1210), already localized.
 *
 * @property illuminatedFraction 0 (new) … 1 (full) of the disc lit.
 * @property rotationDegrees 180 where the observer sees the lit limb mirrored (southern hemisphere), else 0.
 * @property illumination the lit share as a whole percent in the language's digits, without the percent sign.
 * @property nextFullMoon the day of the next full moon in the primary calendar, or `null` when none was found.
 * @property nextNewMoon the day of the next new moon in the primary calendar, or `null` when none was found.
 */
data class WidgetMoon(
    val illuminatedFraction: Float,
    val waxing: Boolean,
    val rotationDegrees: Float,
    val illumination: String,
    val nextFullMoon: String?,
    val nextNewMoon: String?,
)

/** Darkness of the map widget (T-1211) on a [columns] × [rows] grid, row by row from the north-west corner. */
data class WidgetMapShade(
    val columns: Int,
    val rows: Int,
    val darkness: ImmutableList<Float>,
)

/**
 * The day/night world map of the map widget (T-1211) in normalized map coordinates (0‥1 from 180° W and from 90° N):
 * the [land] rings, the night [shade] and the chosen place as [marker].
 */
data class WidgetMap(
    val land: ImmutableList<ImmutableList<NormalizedPoint>>,
    val shade: WidgetMapShade?,
    val marker: NormalizedPoint?,
)

/** Builds the Moon and map widgets' content (T-1210, T-1211). */
object WidgetSkyBuilder {
    private const val PERCENT = 100
    private const val HALF_TURN = 180f
    private const val MIN_RING_POINTS = 2

    /** The Moon is new or full at least once in any 30 days, so a slightly longer search always finds both. */
    private val SEARCH: Duration = 32.days

    /** Without a place, the Moon is drawn as seen from the northern hemisphere. */
    private val NORTH = Coordinates(0.0, 0.0)

    /** The Moon at [now] seen from [observer]; the next phases are dated in [zone] and [calendar]. */
    fun moon(
        now: Instant,
        observer: Coordinates?,
        calendar: CalendarArithmetic,
        zone: TimeZone,
        language: LanguageSpec,
    ): WidgetMoon {
        val appearance = Sky.moonAppearance(now, observer ?: NORTH)
        val quarters = Sky.moonQuarters(now, now + SEARCH)

        fun next(quarter: MoonQuarter): String? =
            quarters.firstOrNull { it.quarter == quarter }?.let { event ->
                WidgetContentBuilder.dayTitle(calendar, event.instant.toJdn(zone), language)
            }
        val percent = (appearance.illuminatedFraction * PERCENT).roundToInt()
        return WidgetMoon(
            illuminatedFraction = appearance.illuminatedFraction.toFloat(),
            waxing = appearance.waxing,
            rotationDegrees = if (appearance.brightLimbOnRight == appearance.waxing) 0f else HALF_TURN,
            illumination = Numerals.localizeDigits(percent.toString(), language.numerals),
            nextFullMoon = next(MoonQuarter.FULL_MOON),
            nextNewMoon = next(MoonQuarter.NEW_MOON),
        )
    }

    /**
     * The map from [land] rings of flattened normalized (x, y) pairs, the [darkness] (0 clear … 1 night) of each cell
     * of a [columns] × [rows] grid, and the [marker]; rings with fewer than two points are dropped.
     */
    fun map(
        land: List<FloatArray>,
        columns: Int,
        rows: Int,
        darkness: (column: Int, row: Int) -> Float,
        marker: NormalizedPoint?,
    ): WidgetMap {
        val rings =
            land
                .filter { it.size >= 2 * MIN_RING_POINTS }
                .map { ring ->
                    List(ring.size / 2) { NormalizedPoint(ring[2 * it], ring[2 * it + 1]) }.toImmutableList()
                }
        val shade =
            if (columns > 0 && rows > 0) {
                WidgetMapShade(
                    columns,
                    rows,
                    List(columns * rows) { darkness(it % columns, it / columns) }.toImmutableList(),
                )
            } else {
                null
            }
        return WidgetMap(rings.toImmutableList(), shade, marker)
    }
}
