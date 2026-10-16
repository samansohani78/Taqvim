/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth

/** T-1201: the 1×1 date widget — weekday, the day number and the secondary date. */
class DateWidget1x1 : TaqvimGlanceWidget(WidgetKind.DATE_1X1) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        WidgetDateLines(data, config, style, compact = true, size = size)
    }
}

/**
 * T-1202: the 4×1 date and clock widget. The clock is a platform `TextClock` (through Glance's `AndroidRemoteViews`),
 * which the launcher ticks itself, so the widget needs no wake-up per minute; the date follows the day change.
 */
class DateClockWidget4x1 : TaqvimGlanceWidget(WidgetKind.DATE_CLOCK_4X1) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetClock(style, GlanceModifier.wrapContentWidth())
            Spacer(GlanceModifier.width(CLOCK_GAP_DP.dp))
            // The clock is an AndroidRemoteViews, and Glance translates one as a container that claims the row:
            // without a weight here the date column measured zero wide at the widget's own 4x1 span and the user
            // saw a clock and nothing else (`EveryWidgetFitsItsBoxTest`, 2026-10-17). The weight gives the dates
            // whatever the clock did not take.
            WidgetDateLines(
                data,
                config,
                style,
                compact = false,
                size = size,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

/** A self-updating clock in the widget's text color and scale (12/24-hour format follows the device setting). */
@Composable
@GlanceComposable
fun WidgetClock(
    style: WidgetStyle,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val views =
        RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setTextColor(R.id.widget_clock, style.foreground.getColor(context).toArgb())
            setTextViewTextSize(
                R.id.widget_clock,
                TypedValue.COMPLEX_UNIT_SP,
                WidgetAppearance.scaled(CLOCK_SP, style.scalePercent),
            )
        }
    AndroidRemoteViews(views, modifier = modifier)
}

private const val CLOCK_SP = 36f
private const val CLOCK_GAP_DP = 12
