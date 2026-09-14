/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.events.DayEvents
import ir.taqvim.feature.widgets.WidgetCalendarBuilder
import ir.taqvim.feature.widgets.WidgetCalendarInputs
import ir.taqvim.feature.widgets.WidgetData
import ir.taqvim.feature.widgets.WidgetDayFacts
import ir.taqvim.feature.widgets.WidgetDayInputs
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetView
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The days of the month, week and schedule widgets (T-1205…T-1208) with their holidays, weekends and events from the
 * events repository, loaded for only the days each widget shows.
 */
internal class WidgetCalendarParts(
    private val rangeEvents: (JdnRange) -> Flow<List<DayEvents>>,
) {
    /** [data] with the calendar part of [kind]; the interactive month shows the month of [view], the others today's. */
    suspend fun addTo(
        data: WidgetData,
        kind: WidgetKind,
        day: WidgetDayInputs,
        weekStart: Weekday,
        view: WidgetView,
    ): WidgetData {
        val inputs = WidgetCalendarInputs(day.jdn, day.language, day.primary, day.secondary, weekStart)
        val language = day.language.code
        return when (kind) {
            WidgetKind.MONTH_INTERACTIVE, WidgetKind.MONTH_BITMAP -> {
                val offset = if (kind == WidgetKind.MONTH_INTERACTIVE) view.monthOffset else 0
                val facts = facts(WidgetCalendarBuilder.monthRange(inputs, offset), language)
                data.copy(month = WidgetCalendarBuilder.month(inputs, offset, facts))
            }

            WidgetKind.WEEK_STRIP -> {
                val facts = facts(WidgetCalendarBuilder.weekRange(inputs), language)
                data.copy(week = WidgetCalendarBuilder.week(inputs, facts).toImmutableList())
            }

            WidgetKind.SCHEDULE -> {
                val facts = facts(WidgetCalendarBuilder.scheduleRange(inputs), language)
                data.copy(schedule = WidgetCalendarBuilder.schedule(inputs, facts).toImmutableList())
            }

            else -> {
                data
            }
        }
    }

    private suspend fun facts(
        range: JdnRange,
        language: String,
    ): (Jdn) -> WidgetDayFacts {
        val days = rangeEvents(range).first().associateBy { it.jdn }
        return { jdn ->
            days[jdn]?.let { WidgetDayFacts(it.isHoliday, it.isWeekend, it.widgetEventLines(language)) }
                ?: WidgetDayFacts.NONE
        }
    }
}
