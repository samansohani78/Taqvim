/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.agenda.AgendaDay
import ir.taqvim.feature.agenda.AgendaDaySource
import ir.taqvim.feature.agenda.AgendaEvent
import ir.taqvim.feature.agenda.AgendaEventKind
import ir.taqvim.feature.agenda.AgendaSettings
import ir.taqvim.feature.agenda.AgendaSettingsSource
import ir.taqvim.feature.calendar.DayEventKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The month list's preferences (T-901) from the stored user preferences (T-600). */
internal class PreferencesAgendaSettingsSource(
    private val preferences: UserPreferencesRepository,
) : AgendaSettingsSource {
    override fun settings(): Flow<AgendaSettings> =
        preferences.preferences
            .map { AgendaSettings(it.calendars, it.islamicVariant, it.languageCode, it.islamicOverride.table) }
            .distinctUntilChanged()
}

/** Days of the month list (T-901) from the events repository (T-305), titled in [language]. */
internal class RepositoryAgendaDaySource(
    private val days: (JdnRange) -> Flow<List<DayEvents>>,
    private val language: Flow<String>,
) : AgendaDaySource {
    override fun days(range: JdnRange): Flow<List<AgendaDay>> =
        combine(days.invoke(range), language.distinctUntilChanged()) { days, language ->
            days.map { it.toAgendaDay(language) }
        }
}

/** This day for the month list, with the calendar screen's event order and titles (T-801). */
internal fun DayEvents.toAgendaDay(language: String): AgendaDay {
    val day = toCalendarDay(language)
    return AgendaDay(
        jdn = day.jdn,
        isHoliday = day.isHoliday,
        isWeekend = day.isWeekend,
        events = day.events.map { AgendaEvent(it.id, it.kind.toAgendaKind(), it.title, it.isHoliday) },
    )
}

private fun DayEventKind.toAgendaKind(): AgendaEventKind =
    when (this) {
        DayEventKind.OFFICIAL -> AgendaEventKind.OFFICIAL
        DayEventKind.PERSONAL -> AgendaEventKind.PERSONAL
        DayEventKind.DEVICE -> AgendaEventKind.DEVICE
        DayEventKind.SUBSCRIPTION -> AgendaEventKind.SUBSCRIPTION
    }
