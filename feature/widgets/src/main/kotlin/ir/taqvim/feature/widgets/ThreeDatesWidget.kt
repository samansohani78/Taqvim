/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
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
        val lines = data.calendarDates.take(MAX_LINES).ifEmpty { listOf(data.title) }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lines.forEachIndexed { index, line ->
                Text(
                    text = BidiText.isolate(line),
                    style = style.text(if (index == 0) primarySp(size) else secondarySp(size), bold = index == 0),
                    maxLines = 2,
                )
            }
        }
    }

    private fun primarySp(size: WidgetSize): Float =
        if (size ==
            WidgetSize.EXTRA_LARGE
        ) {
            PRIMARY_SP
        } else {
            SMALL_PRIMARY_SP
        }

    private fun secondarySp(size: WidgetSize): Float =
        if (size == WidgetSize.EXTRA_LARGE) SECONDARY_SP else SMALL_SECONDARY_SP

    private companion object {
        /** Three calendars is the concept; a user who keeps more sees the three they put first. */
        const val MAX_LINES = 3
        const val PRIMARY_SP = 16f
        const val SECONDARY_SP = 12f
        const val SMALL_PRIMARY_SP = 13f
        const val SMALL_SECONDARY_SP = 10f
    }
}
