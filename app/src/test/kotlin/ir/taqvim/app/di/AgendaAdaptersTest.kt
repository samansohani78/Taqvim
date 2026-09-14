/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.IcsOccurrence
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.agenda.AgendaDay
import ir.taqvim.feature.agenda.AgendaEvent
import ir.taqvim.feature.agenda.AgendaEventKind
import ir.taqvim.feature.agenda.AgendaSettings
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-901 wiring: the month list's settings and days from the preferences and the events repository. */
class AgendaAdaptersTest {
    private val nowruz = OfficialEvents.ALL.first { it.id.value == NOWRUZ }
    private val nowruz1405 = LocalDate(2026, 3, 21).toJdn()

    @Test
    fun `settings follow the calendars, Islamic variant and language`(): Unit =
        runTest {
            val preferences = UserPreferences.defaultsFor("fa").copy(islamicVariant = IslamicVariant.UMM_AL_QURA)

            PreferencesAgendaSettingsSource(repositoryOf(preferences)).settings().first() shouldBe
                AgendaSettings(preferences.calendars, IslamicVariant.UMM_AL_QURA, "fa")
        }

    @Test
    fun `days keep the calendar screen's order, kinds and titles in the current language`(): Unit =
        runTest {
            val language = MutableStateFlow("en")
            val source = RepositoryAgendaDaySource({ flowOf(listOf(day())) }, language)
            val range = nowruz1405..nowruz1405

            source.days(range).first() shouldBe
                listOf(
                    AgendaDay(
                        jdn = nowruz1405,
                        isHoliday = true,
                        isWeekend = false,
                        events =
                            listOf(
                                AgendaEvent(NOWRUZ, AgendaEventKind.OFFICIAL, nowruz.title.forLanguage("en"), true),
                                AgendaEvent("7", AgendaEventKind.PERSONAL, "Birthday", false),
                                AgendaEvent("3", AgendaEventKind.DEVICE, "Meeting", false),
                                AgendaEvent("2:uid-1", AgendaEventKind.SUBSCRIPTION, "Talk", false),
                            ),
                    ),
                )

            language.value = "fa"
            source
                .days(range)
                .first()
                .single()
                .events
                .first()
                .title shouldBe nowruz.title.forLanguage("fa")
        }

    private fun day(): DayEvents {
        val begin = Instant.parse("2026-03-21T06:30:00Z")
        val end = Instant.parse("2026-03-21T07:30:00Z")
        val days = nowruz1405..nowruz1405
        return DayEvents(
            jdn = nowruz1405,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1),
            hijri = null,
            official = listOf(Occurrence(nowruz, nowruz1405, PERSIAN_NOWRUZ, isHoliday = true, year = 1405)),
            isHoliday = true,
            isWeekend = false,
            personal =
                listOf(
                    PersonalOccurrence(
                        eventId = 7,
                        title = "Birthday",
                        notes = "",
                        calendarSystem = CalendarSystem.PERSIAN,
                        days = days,
                        startMinute = null,
                        endMinute = null,
                        timeZoneId = "Asia/Tehran",
                        colorArgb = null,
                        recurring = false,
                    ),
                ),
            device = listOf(DeviceEvent(3, 1, "Meeting", begin, end, allDay = false, colorArgb = null, days)),
            ics = listOf(IcsOccurrence(2, "uid-1", "Talk", "", begin, end, allDay = false, days = days)),
        )
    }

    private companion object {
        const val NOWRUZ = "ir.holiday.nowruz-1"
        val PERSIAN_NOWRUZ = CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)
    }
}
