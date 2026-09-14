/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.Text
import ir.taqvim.core.ui.painter.ProgressRingBitmapModel
import kotlin.math.max
import kotlin.math.min

/** T-1210: the Moon's phase; medium widgets add the lit share and the next full and new moon. Opens astronomy. */
class MoonWidget : TaqvimGlanceWidget(WidgetKind.MOON) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        val moon = data.moon
        if (moon == null) {
            WidgetMessage(context.getString(R.string.widget_load_failed), style)
            return
        }
        val open = actionStartActivity(WidgetLinks.intent(context, WidgetClickTarget.Astronomy))
        val lit = context.getString(R.string.widget_moon_illumination, moon.illumination)
        val compact = size == WidgetSize.SMALL
        val painters = WidgetPainters.of(context, GlanceTheme.colors, config)
        val density = context.resources.displayMetrics.density
        val (width, height) = WidgetDrawings.pixels(LocalSize.current, density, if (compact) 0f else MOON_TEXT_DP)
        val side = min(width, height)
        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(open),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(painters.moon.paint(WidgetDrawings.moonModel(moon), side, side)),
                contentDescription = context.getString(R.string.widget_moon_content, lit),
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
            if (!compact) {
                Text(lit, style = style.text(SMALL_SP, bold = true), maxLines = 1)
                moon.nextFullMoon?.let {
                    Text(
                        context.getString(R.string.widget_moon_next_full, it),
                        style = style.text(SMALL_SP),
                        maxLines = 1,
                    )
                }
                moon.nextNewMoon?.let {
                    Text(
                        context.getString(R.string.widget_moon_next_new, it),
                        style = style.text(SMALL_SP),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** T-1211: day and night on the world map with the chosen place; opens the map. */
class MapWidget : TaqvimGlanceWidget(WidgetKind.MAP) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        val map = data.map
        if (map == null) {
            WidgetMessage(context.getString(R.string.widget_load_failed), style)
            return
        }
        val open = actionStartActivity(WidgetLinks.intent(context, WidgetClickTarget.WorldMap))
        val painters = WidgetPainters.of(context, GlanceTheme.colors, config)
        val density = context.resources.displayMetrics.density
        val (width, height) = WidgetDrawings.pixels(LocalSize.current, density, 0f)
        val description = if (map.marker == null) R.string.widget_map_content else R.string.widget_map_content_place
        Image(
            provider = ImageProvider(painters.map.paint(WidgetDrawings.mapModel(map), width, height)),
            contentDescription = context.getString(description),
            modifier = GlanceModifier.fillMaxSize().clickable(open),
        )
    }
}

/** T-1212: the days until or the time since the chosen date, with a progress ring; opens that day. */
class CountdownWidget : TaqvimGlanceWidget(WidgetKind.COUNTDOWN) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        val countdown = data.countdown
        if (countdown == null) {
            WidgetMessage(context.getString(R.string.widget_countdown_setup), style)
            return
        }
        val open = actionStartActivity(WidgetLinks.intent(context, WidgetClickTarget.Day(countdown.date)))
        val texts = CountdownTexts.of(context.resources, countdown)
        val compact = size == WidgetSize.SMALL
        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(open),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) Text(countdown.title, style = style.text(SMALL_SP, bold = true), maxLines = 1)
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                if (!compact) CountdownRing(countdown, config)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(texts.headline, style = style.text(NUMBER_SP, bold = true), maxLines = 1)
                    if (texts.unit.isNotEmpty()) Text(texts.unit, style = style.text(SMALL_SP), maxLines = 1)
                }
            }
            if (!compact) texts.detail?.let { Text(it, style = style.text(SMALL_SP), maxLines = 1) }
        }
    }
}

@Composable
@GlanceComposable
private fun CountdownRing(
    countdown: WidgetCountdownView,
    config: WidgetConfig,
) {
    val context = LocalContext.current
    val painters = WidgetPainters.of(context, GlanceTheme.colors, config)
    val density = context.resources.displayMetrics.density
    val (width, height) = WidgetDrawings.pixels(LocalSize.current, density, COUNTDOWN_TEXT_DP)
    val side = max(1, min(width, height))
    Image(
        provider = ImageProvider(painters.ring.paint(ProgressRingBitmapModel(countdown.progress), side, side)),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxSize(),
    )
}

/** The countdown widget's texts: the [headline] amount, its [unit] and the optional [detail] line. */
internal data class CountdownTexts(
    val headline: String,
    val unit: String,
    val detail: String?,
) {
    companion object {
        fun of(
            resources: Resources,
            countdown: WidgetCountdownView,
        ): CountdownTexts {
            val headline = countdown.headline
            if (countdown.status == CountdownStatus.TODAY) {
                return CountdownTexts(resources.getString(R.string.widget_countdown_today), "", null)
            }
            val unit =
                when (countdown.status) {
                    CountdownStatus.PASSED -> R.plurals.widget_countdown_unit_ago
                    CountdownStatus.ELAPSED -> unitLabel(headline.unit)
                    else -> R.plurals.widget_countdown_unit_left
                }
            val detail =
                countdown.parts
                    .map { resources.getQuantityString(partLabel(it.unit), it.count, it.text) }
                    .reduceOrNull { joined, next -> resources.getString(R.string.widget_countdown_join, joined, next) }
            return CountdownTexts(headline.text, resources.getQuantityString(unit, headline.count), detail)
        }

        private fun unitLabel(unit: CountdownUnit): Int =
            when (unit) {
                CountdownUnit.YEARS -> R.plurals.widget_countdown_unit_years
                CountdownUnit.MONTHS -> R.plurals.widget_countdown_unit_months
                CountdownUnit.WEEKS, CountdownUnit.DAYS -> R.plurals.widget_countdown_unit_days
            }

        private fun partLabel(unit: CountdownUnit): Int =
            when (unit) {
                CountdownUnit.YEARS -> R.plurals.widget_countdown_years
                CountdownUnit.MONTHS -> R.plurals.widget_countdown_months
                CountdownUnit.WEEKS -> R.plurals.widget_countdown_weeks
                CountdownUnit.DAYS -> R.plurals.widget_countdown_days
            }
    }
}

private const val SMALL_SP = 12f
private const val NUMBER_SP = 28f
private const val MOON_TEXT_DP = 48f
private const val COUNTDOWN_TEXT_DP = 32f
