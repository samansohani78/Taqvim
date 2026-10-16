/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import ir.taqvim.core.i18n.BidiText

/**
 * T-1216: today's date in each of the user's calendars, one per line, and nothing else.
 *
 * The whole widget is three strings the calendar engines produce from a day number, so it needs no database, no
 * events and no network: [WidgetKind.THREE_DATES] declares the date as its only dependency and offers no optional
 * content, which means the loader fetches nothing for it and the host never has to draw a placeholder.
 *
 * The first line is the user's primary calendar and is the largest; the rest follow in their chosen order, smaller.
 * Each date is directionally isolated so a Latin Gregorian month in an RTL host retains its own reading order.
 * The Islamic line follows the user's chosen method and any official
 * override, because it comes from the same arithmetic the rest of the app uses.
 */
class ThreeDatesWidget : TaqvimGlanceWidget(WidgetKind.THREE_DATES) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val fontScale = LocalContext.current.resources.configuration.fontScale
        val lines = data.calendarDates.take(linesFor(size, fontScale)).ifEmpty { listOf(data.title) }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The weekday is what a glance is usually for, and it costs nothing this widget does not already
            // have: it comes from the same day number as the dates, so the DATE-only dependency is unchanged.
            // It is the first thing dropped when the box is crowded, because a date without its weekday is
            // still the date.
            if (showsWeekday(size, fontScale)) {
                Text(data.weekday, style = style.text(secondarySp(size)), maxLines = 1)
            }
            lines.forEachIndexed { index, line ->
                Text(
                    text = BidiText.isolate(line),
                    style = style.text(if (index == 0) primarySp(size) else secondarySp(size), bold = index == 0),
                    maxLines = 2,
                    // A gap under the headline so the two smaller calendars read as subordinate to it rather
                    // than as a cramped stack. Only under the first line: the other two belong together.
                    modifier = if (index == 1) GlanceModifier.padding(top = LINE_GAP_DP.dp) else GlanceModifier,
                )
            }
        }
    }

    /** The weekday fits whenever all three calendars do; below that the dates have the room. */
    private fun showsWeekday(
        size: WidgetSize,
        fontScale: Float,
    ): Boolean = linesFor(size, fontScale) == MAX_LINES

    /**
     * How many of the three calendars fit.
     *
     * Three date lines do not fit two cells at a large font scale — not at any readable size: at font scale 2.0 in
     * a 110 dp box even 9 sp wraps, because the width binds, and a wrapped line then has nowhere to go. Clipping
     * the user's dates to honour the widget's name would be the wrong way round, so the widget shows fewer
     * calendars instead, largest first. The user asked for big text; they get big text, and the date they read
     * first is the one that survives.
     *
     * A widget resized past its default span has the room and keeps all three.
     */
    private fun linesFor(
        size: WidgetSize,
        fontScale: Float,
    ): Int =
        when {
            size != WidgetSize.MEDIUM -> MAX_LINES
            fontScale >= CROWDED_FONT_SCALE -> MAX_LINES - 2
            fontScale > 1f -> MAX_LINES - 1
            else -> MAX_LINES
        }

    private fun primarySp(size: WidgetSize): Float =
        when (size) {
            WidgetSize.EXTRA_LARGE -> PRIMARY_SP
            WidgetSize.MEDIUM_LARGE -> MEDIUM_PRIMARY_SP
            else -> SMALL_PRIMARY_SP
        }

    private fun secondarySp(size: WidgetSize): Float =
        when (size) {
            WidgetSize.EXTRA_LARGE -> SECONDARY_SP
            WidgetSize.MEDIUM_LARGE -> MEDIUM_SECONDARY_SP
            else -> SMALL_SECONDARY_SP
        }

    private companion object {
        /** Three calendars is the concept; a user who keeps more sees the three they put first. */
        const val MAX_LINES = 3

        /**
         * Type sized for the widget the user actually gets.
         *
         * The widget is two cells square, so [WidgetSize.MEDIUM] is its own size and 13 sp over 10 sp fills it.
         * It shipped as a 3×3 with those same numbers, which is where this went wrong: `forAvailable` picks the
         * largest supported bucket that *fits*, the kind offered only MEDIUM (110 dp) and EXTRA_LARGE (250 dp),
         * and 180 dp fits only the first — so a 3×3 widget drew type meant for a widget a third its area. The
         * answer was both halves: the widget is the size its content wants, and every span it can reach now has
         * a bucket with type of its own.
         *
         * The ceiling is `ThreeDatesRenderingTest`, which requires all three dates to fit unwrapped at font scale
         * 2.0 in 180 dp. The secondary lines bind it — the longest is an Islamic date like "28 Rabiʻ II 1448" —
         * so the primary line can be much larger than them without breaking that contract, which is where the
         * hierarchy below comes from. Raising the secondary lines past 11 sp fails it, and the contract is an
         * accessibility promise, not an obstacle: it is not relaxed to make the type bigger.
         */
        const val PRIMARY_SP = 32f
        const val SECONDARY_SP = 16f

        /** The 3x3 span this widget targets, and the smallest a host should ever give it. */
        const val MEDIUM_PRIMARY_SP = 26f
        const val MEDIUM_SECONDARY_SP = 11f

        /** Above this the two cells hold one date only; between 1.0 and it, two. */
        const val CROWDED_FONT_SCALE = 1.5f

        /** Space between the headline date and the two calendars under it. */
        const val LINE_GAP_DP = 6

        /** The default two-cell span: 110 dp, which is all three dates at the default font scale. */
        const val SMALL_PRIMARY_SP = 13f
        const val SMALL_SECONDARY_SP = 10f
    }
}
