/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.Text

/** T-1203: the 2×2 day summary — date lines, the day's events and the next prayer time. */
class DaySummaryWidget2x2 : TaqvimGlanceWidget(WidgetKind.DAY_SUMMARY_2X2) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetDateLines(data, config, style, compact = false)
            if (config.shows(WidgetContent.EVENTS)) {
                WidgetEventList(data, style, if (size == WidgetSize.LARGE) LARGE_EVENTS else MEDIUM_EVENTS)
            }
            if (config.shows(WidgetContent.NEXT_PRAYER)) WidgetNextPrayer(data, style)
        }
    }

    companion object {
        /** Event lines shown in the 2×2 and the enlarged 4×2 layouts. */
        const val MEDIUM_EVENTS: Int = 2
        const val LARGE_EVENTS: Int = 4
    }
}

/** T-1204: the 4×2 prayer strip — date lines above the day's prayer times, the next one highlighted. */
class PrayerStripWidget4x2 : TaqvimGlanceWidget(WidgetKind.PRAYER_STRIP_4X2) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A widget shrunk to one row keeps only the strip.
            if (size != WidgetSize.WIDE) WidgetDateLines(data, config, style, compact = false)
            WidgetPrayerStrip(data, config, style)
        }
    }
}

/** Up to [maxEvents] event lines; personal events open in the editor, others open their day. */
@Composable
@GlanceComposable
fun WidgetEventList(
    data: WidgetData,
    style: WidgetStyle,
    maxEvents: Int,
) {
    val context = LocalContext.current
    if (data.events.isEmpty()) {
        Text(context.getString(R.string.widget_no_events), style = style.text(SMALL_SP), maxLines = 1)
        return
    }
    data.events.take(maxEvents).forEach { event ->
        val target = event.eventId?.let { WidgetClickTarget.Event(it) } ?: WidgetClickTarget.Day(data.date)
        Text(
            text = event.title,
            modifier = GlanceModifier.clickable(actionStartActivity(WidgetLinks.intent(context, target))),
            style = style.text(SMALL_SP, holiday = event.isHoliday),
            maxLines = 1,
        )
    }
}

/** "Maghrib 19:12", or a prompt to choose a place; opens the Times tab. */
@Composable
@GlanceComposable
fun WidgetNextPrayer(
    data: WidgetData,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val next = data.nextPrayer
    val text =
        next?.let { context.getString(R.string.widget_next_prayer, it.name, it.time) }
            ?: context.getString(R.string.widget_no_place)
    Text(
        text = text,
        modifier = openPrayerTimes(context),
        style = style.text(SMALL_SP, bold = true),
        maxLines = 1,
    )
}

/** The day's prayer times side by side; the next one is bold when the widget shows the next prayer. */
@Composable
@GlanceComposable
fun WidgetPrayerStrip(
    data: WidgetData,
    config: WidgetConfig,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val openTimes = openPrayerTimes(context)
    if (data.prayers.isEmpty()) {
        Text(
            text = context.getString(R.string.widget_no_place),
            modifier = openTimes,
            style = style.text(SMALL_SP),
            maxLines = NO_PLACE_LINES,
        )
        return
    }
    Row(modifier = openTimes.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        data.prayers.forEach { prayer ->
            val highlight = prayer.isNext && config.shows(WidgetContent.NEXT_PRAYER)
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(prayer.name, style = style.text(TINY_SP, bold = highlight), maxLines = 1)
                Text(prayer.time, style = style.text(SMALL_SP, bold = highlight), maxLines = 1)
            }
        }
    }
}

/** Tapping opens the Times tab. */
private fun openPrayerTimes(context: Context): GlanceModifier =
    GlanceModifier.clickable(actionStartActivity(WidgetLinks.intent(context, WidgetClickTarget.PrayerTimes)))

private const val SMALL_SP = 12f
private const val TINY_SP = 10f
private const val NO_PLACE_LINES = 2
