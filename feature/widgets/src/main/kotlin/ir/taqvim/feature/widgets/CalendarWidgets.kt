/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** T-1205: the interactive month — previous, next, today and new-event controls above a tappable month grid. */
class MonthInteractiveWidget : TaqvimGlanceWidget(WidgetKind.MONTH_INTERACTIVE) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val month = data.month
        if (month == null) {
            WidgetMessage(LocalContext.current.getString(R.string.widget_load_failed), style)
            return
        }
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            MonthControls(data, month, config, style)
            WidgetMonthGrid(month, config, style, withSecondary = size == WidgetSize.EXTRA_LARGE)
        }
    }
}

/** T-1207: the week strip — today's week from the week start, each day opening in the calendar. */
class WeekStripWidget : TaqvimGlanceWidget(WidgetKind.WEEK_STRIP) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            data.week.forEach { day ->
                WidgetDayCell(day, config, style, GlanceModifier.defaultWeight(), DayCellParts(weekday = true))
            }
        }
    }
}

/** T-1208: the schedule — the events of today and the next thirteen days as a scrolling list. */
class ScheduleWidget : TaqvimGlanceWidget(WidgetKind.SCHEDULE) {
    @Composable
    @GlanceComposable
    override fun Content(
        data: WidgetData,
        config: WidgetConfig,
        style: WidgetStyle,
        size: WidgetSize,
    ) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                context.getString(R.string.widget_schedule_title),
                style = style.text(TITLE_SP, bold = true),
                maxLines = 1,
            )
            if (data.schedule.all { it.events.isEmpty() }) {
                Text(context.getString(R.string.widget_schedule_empty), style = style.text(SMALL_SP), maxLines = 2)
                return@Column
            }
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(ScheduleRows.of(data.schedule), itemId = { it.id }) { row -> ScheduleRowText(row, config, style) }
            }
        }
    }
}

/** The rows of the schedule list: a header per day, then its events. */
sealed interface ScheduleRow {
    val id: Long

    data class Header(
        val day: WidgetScheduleDay,
    ) : ScheduleRow {
        override val id: Long = day.date.toEpochDays() * ROWS_PER_DAY
    }

    data class Event(
        val day: WidgetScheduleDay,
        val event: WidgetEventLine,
        val index: Int,
    ) : ScheduleRow {
        override val id: Long = day.date.toEpochDays() * ROWS_PER_DAY + index + 1
    }

    companion object {
        /** Ids stay unique for up to this many rows per day. */
        const val ROWS_PER_DAY: Long = 1000
    }
}

/** Flattens schedule days into list rows. */
object ScheduleRows {
    fun of(days: List<WidgetScheduleDay>): List<ScheduleRow> =
        days.flatMap { day ->
            listOf(ScheduleRow.Header(day)) +
                day.events.mapIndexed { index, event -> ScheduleRow.Event(day, event, index) }
        }
}

@Composable
@GlanceComposable
private fun ScheduleRowText(
    row: ScheduleRow,
    config: WidgetConfig,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val holidays = config.shows(WidgetContent.HOLIDAYS)
    when (row) {
        is ScheduleRow.Header -> {
            Text(
                text = row.day.title,
                modifier = GlanceModifier.clickable(open(context, WidgetClickTarget.Day(row.day.date))),
                style = style.text(SMALL_SP, holiday = holidays && row.day.isHoliday, bold = true),
                maxLines = 1,
            )
        }

        is ScheduleRow.Event -> {
            val target = row.event.eventId?.let { WidgetClickTarget.Event(it) } ?: WidgetClickTarget.Day(row.day.date)
            Text(
                text = row.event.title,
                modifier = GlanceModifier.clickable(open(context, target)).padding(start = INDENT_DP.dp),
                style = style.text(SMALL_SP, holiday = holidays && row.event.isHoliday),
                maxLines = 1,
            )
        }
    }
}

@Composable
@GlanceComposable
private fun MonthControls(
    data: WidgetData,
    month: WidgetMonth,
    config: WidgetConfig,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val back = context.getString(R.string.widget_glyph_back)
    val forward = context.getString(R.string.widget_glyph_forward)
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ControlText(if (rtl) forward else back, context.getString(R.string.widget_month_previous), step(-1), style)
        Column(
            modifier = GlanceModifier.defaultWeight().clickable(open(context, WidgetClickTarget.Day(month.firstDay))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(month.title, style = style.text(TITLE_SP, bold = true), maxLines = 1)
            val secondary = month.secondaryTitle
            if (config.shows(WidgetContent.SECONDARY_DATE) && secondary != null) {
                Text(secondary, style = style.text(TINY_SP), maxLines = 1)
            }
        }
        ControlText(if (rtl) back else forward, context.getString(R.string.widget_month_next), step(1), style)
        val today = context.getString(R.string.widget_month_today)
        ControlText(today, today, step(0), style)
        val newEventDay = if (month.offset == 0) data.date else month.firstDay
        ControlText(
            context.getString(R.string.widget_glyph_add),
            context.getString(R.string.widget_month_add),
            open(context, WidgetClickTarget.NewEvent(newEventDay)),
            style,
        )
    }
}

@Composable
@GlanceComposable
private fun ControlText(
    text: String,
    description: String,
    action: Action,
    style: WidgetStyle,
) {
    Text(
        text = text,
        modifier =
            GlanceModifier
                .clickable(action)
                .semantics { contentDescription = description }
                .padding(horizontal = CONTROL_PADDING_DP.dp),
        style = style.text(CONTROL_SP, bold = true),
        maxLines = 1,
    )
}

/** The weekday row and six week rows of [month]; days show their secondary day when [withSecondary]. */
@Composable
@GlanceComposable
fun ColumnScope.WidgetMonthGrid(
    month: WidgetMonth,
    config: WidgetConfig,
    style: WidgetStyle,
    withSecondary: Boolean,
) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        month.weekdayLabels.forEach { label ->
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = style.text(TINY_SP), maxLines = 1)
            }
        }
    }
    month.days.chunked(WidgetCalendarBuilder.WEEK_DAYS).forEach { week ->
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            week.forEach { day ->
                val parts = DayCellParts(secondary = withSecondary)
                WidgetDayCell(day, config, style, GlanceModifier.defaultWeight(), parts)
            }
        }
    }
}

/** Which optional lines a day cell draws besides the day number. */
data class DayCellParts(
    val weekday: Boolean = false,
    val secondary: Boolean = false,
)

/** A tappable day: optional weekday, the day number in its tone, optional secondary day and an event dot. */
@Composable
@GlanceComposable
fun WidgetDayCell(
    day: WidgetCalendarDay,
    config: WidgetConfig,
    style: WidgetStyle,
    modifier: GlanceModifier,
    parts: DayCellParts,
) {
    val context = LocalContext.current
    val spoken =
        if (day.isHoliday) context.getString(R.string.widget_day_holiday, day.description) else day.description
    Column(
        modifier =
            modifier
                .clickable(open(context, WidgetClickTarget.Day(day.date)))
                .semantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (parts.weekday) Text(day.weekdayLabel, style = style.text(TINY_SP), maxLines = 1)
        Text(day.dayLabel, style = dayTextStyle(context, day, config, style), maxLines = 1)
        val secondary = day.secondaryLabel
        if (parts.secondary && config.shows(WidgetContent.SECONDARY_DATE) && secondary != null) {
            Text(secondary, style = style.text(TINY_SP), maxLines = 1)
        }
        if (config.shows(WidgetContent.EVENTS) && day.eventCount > 0) {
            Text(context.getString(R.string.widget_glyph_event), style = style.text(TINY_SP, bold = true), maxLines = 1)
        }
    }
}

/** Holidays and weekends in the holiday color, days of other months muted, today bold and underlined. */
private fun dayTextStyle(
    context: Context,
    day: WidgetCalendarDay,
    config: WidgetConfig,
    style: WidgetStyle,
): TextStyle {
    val toned = config.shows(WidgetContent.HOLIDAYS) && (day.isHoliday || day.isWeekend)
    val base = style.text(DAY_SP, holiday = toned, bold = day.isToday)
    return when {
        !day.inMonth -> {
            TextStyle(
                color = ColorProvider(style.foreground.getColor(context).copy(alpha = MUTED_ALPHA)),
                fontSize = base.fontSize,
                fontWeight = base.fontWeight,
            )
        }

        day.isToday -> {
            TextStyle(
                color = base.color,
                fontSize = base.fontSize,
                fontWeight = base.fontWeight,
                textDecoration = TextDecoration.Underline,
            )
        }

        else -> {
            base
        }
    }
}

private fun step(delta: Int): Action = actionRunCallback<MonthStepCallback>(WidgetMonthStep.parameters(delta))

private fun open(
    context: Context,
    target: WidgetClickTarget,
): Action = actionStartActivity(WidgetLinks.intent(context, target))

private const val TITLE_SP = 14f
private const val SMALL_SP = 12f
private const val TINY_SP = 10f
private const val DAY_SP = 13f
private const val CONTROL_SP = 16f
private const val CONTROL_PADDING_DP = 6
private const val INDENT_DP = 8
private const val MUTED_ALPHA = 0.45f
