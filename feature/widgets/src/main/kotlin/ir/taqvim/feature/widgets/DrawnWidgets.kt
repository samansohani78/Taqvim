/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.Text
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.painter.MapShade
import ir.taqvim.core.ui.painter.MapShape
import ir.taqvim.core.ui.painter.MapThumbnailModel
import ir.taqvim.core.ui.painter.MoonBitmapModel
import kotlin.math.max

/** T-1206: the month drawn as one picture by the T-702 month painter; tapping opens today in the app. */
class MonthBitmapWidget : TaqvimGlanceWidget(WidgetKind.MONTH_BITMAP) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        val month = data.month
        if (month == null) {
            WidgetMessage(context.getString(R.string.widget_load_failed), style)
            return
        }
        val eventColor = GlanceTheme.colors.primary.getColor(context)
        val painters = WidgetPainters.of(context, GlanceTheme.colors, config)
        val density = context.resources.displayMetrics.density
        val (width, height) = WidgetDrawings.pixels(LocalSize.current, density, TITLE_DP)
        val model = WidgetDrawings.monthModel(month, config, eventColor)
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(month.title, style = style.text(TITLE_SP, bold = true), maxLines = 1)
            Image(
                provider = ImageProvider(painters.month.paint(model, width, height)),
                contentDescription = month.title,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
        }
    }
}

/** T-1209: the Sun's path from sunrise to sunset at the chosen place and the next prayer time; opens the Times tab. */
class SunArcWidget : TaqvimGlanceWidget(WidgetKind.SUN_ARC) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        val openTimes = actionStartActivity(WidgetLinks.intent(context, WidgetClickTarget.PrayerTimes))
        val sun = data.sun
        if (sun == null) {
            // With a place chosen, a missing Sun path means a polar day or night there, not a missing place.
            val message = if (data.daylightUnavailable) R.string.widget_no_daylight else R.string.widget_no_place
            Text(
                text = context.getString(message),
                modifier = GlanceModifier.clickable(openTimes),
                style = style.text(SMALL_SP),
                maxLines = MESSAGE_LINES,
            )
            return
        }
        val description = context.getString(R.string.widget_sun_description, sun.sunrise, sun.sunset)
        val showsPrayer = config.shows(WidgetContent.NEXT_PRAYER)
        val painters = WidgetPainters.of(context, GlanceTheme.colors, config)
        val reserved = if (showsPrayer) PRAYER_LINE_DP else 0f
        val density = context.resources.displayMetrics.density
        val (width, height) = WidgetDrawings.pixels(LocalSize.current, density, reserved)
        val model = WidgetDrawings.sunModel(sun, description)
        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(openTimes),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(painters.sunArc.paint(model, width, height)),
                contentDescription = description,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
            if (showsPrayer) WidgetNextPrayer(data, style)
        }
    }
}

/** Painter models and bitmap sizes of the drawn widgets (T-1206, T-1209). */
object WidgetDrawings {
    /** The frame's padding on each side, in dp (see [WidgetFrame]). */
    const val FRAME_PADDING_DP: Float = 8f

    /** The month painter's model of [month] under [config]; days with events get one dot in [eventColor]. */
    fun monthModel(
        month: WidgetMonth,
        config: WidgetConfig,
        eventColor: Color,
    ): MonthGridModel {
        val showsEvents = config.shows(WidgetContent.EVENTS)
        return MonthGridModel(
            weekdayLabels = month.weekdayLabels,
            cells =
                month.days.map { day ->
                    DayCellModel(
                        dayLabel = day.dayLabel,
                        contentDescription = day.description,
                        secondaryLabels =
                            listOfNotNull(day.secondaryLabel.takeIf { config.shows(WidgetContent.SECONDARY_DATE) }),
                        indicators = listOfNotNull(eventColor.takeIf { showsEvents && day.eventCount > 0 }),
                        isToday = day.isToday,
                        isHoliday = config.shows(WidgetContent.HOLIDAYS) && (day.isHoliday || day.isWeekend),
                        inCurrentMonth = day.inMonth,
                    )
                },
        )
    }

    fun sunModel(
        sun: WidgetSun,
        description: String,
    ): SunArcModel = SunArcModel(sun.progress, sun.sunrise, sun.sunset, description)

    /** The Moon painter's model of [moon] (T-1210). */
    fun moonModel(moon: WidgetMoon): MoonBitmapModel =
        MoonBitmapModel(moon.illuminatedFraction, moon.waxing, moon.rotationDegrees)

    /** The map painter's model of [map] (T-1211): land as filled rings, the night shade and the place marker. */
    fun mapModel(map: WidgetMap): MapThumbnailModel =
        MapThumbnailModel(
            shapes = map.land.filter { it.size >= 2 }.map { MapShape(it, closed = true) },
            marker = map.marker,
            shade = map.shade?.let { MapShade(it.columns, it.rows, it.darkness) },
        )

    /** The bitmap size in pixels for a widget of [size] (dp) at [density], leaving [reservedHeightDp] for text. */
    fun pixels(
        size: DpSize,
        density: Float,
        reservedHeightDp: Float,
    ): Pair<Int, Int> {
        val width = (size.width.value - 2 * FRAME_PADDING_DP) * density
        val height = (size.height.value - 2 * FRAME_PADDING_DP - reservedHeightDp) * density
        return max(1, width.toInt()) to max(1, height.toInt())
    }
}

private const val TITLE_SP = 14f
private const val SMALL_SP = 12f
private const val TITLE_DP = 22f
private const val PRAYER_LINE_DP = 18f
private const val MESSAGE_LINES = 3
