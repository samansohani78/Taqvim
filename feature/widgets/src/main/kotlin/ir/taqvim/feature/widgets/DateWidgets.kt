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
        WidgetDateLines(data, config, style, compact = true)
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
            WidgetClock(style)
            Spacer(GlanceModifier.width(CLOCK_GAP_DP.dp))
            WidgetDateLines(data, config, style, compact = false)
        }
    }
}

/** A self-updating clock in the widget's text color and scale (12/24-hour format follows the device setting). */
@Composable
@GlanceComposable
fun WidgetClock(style: WidgetStyle) {
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
    AndroidRemoteViews(views)
}

private const val CLOCK_SP = 36f
private const val CLOCK_GAP_DP = 12
