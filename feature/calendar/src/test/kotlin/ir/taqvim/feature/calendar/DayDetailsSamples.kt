/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventSource
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** Day-details content for Nowruz 1405 with synthetic events (T-802 UI and screenshot tests). */
internal object DayDetailsSamples {
    /** 1 Farvardin 1405, also "today". */
    val nowruz = gregorian(2026, 3, 21)

    /** 10:30 in Tehran on [nowruz]. */
    val morning: Instant = Instant.parse("2026-03-21T07:00:00Z")

    val citation = Citation("https://calendar.ut.ac.ir/Fa/", "Official calendar of Iran 1405 SH", page = "3")

    val official =
        DayEventItem(
            "ir.holiday.nowruz-1",
            DayEventKind.OFFICIAL,
            "Nowruz",
            isHoliday = true,
            source = EventSource.IRAN_OFFICIAL,
            citations = listOf(citation),
        )
    val uncited =
        DayEventItem("un.day", DayEventKind.OFFICIAL, "International day", false, source = EventSource.INTERNATIONAL)
    val personal = DayEventItem("7", DayEventKind.PERSONAL, "Birthday", isHoliday = false)
    val device = DayEventItem("3", DayEventKind.DEVICE, "Meeting", isHoliday = false)
    val subscription = DayEventItem("2:uid-1", DayEventKind.SUBSCRIPTION, "Talk", isHoliday = false)
    val events = listOf(official, uncited, personal, device, subscription)

    fun content(
        languageCode: String = "en",
        tab: DayDetailsTab = DayDetailsTab.CALENDARS,
        events: List<DayEventItem> = this.events,
        place: CalendarPlace? = TEHRAN,
        sourceEvent: DayEventItem? = null,
    ): CalendarContent {
        val settings = PERSIAN_FIRST.copy(languageCode = languageCode)
        val calendars = CalendarCalendars(settings)
        return CalendarContent(
            today = nowruz,
            selectedDay = nowruz,
            calendars = calendars.systems.toImmutableList(),
            selectedDates = calendars.datesOf(nowruz).toImmutableList(),
            monthOffset = 0,
            visibleMonth = calendars.monthStart(nowruz),
            weekStart = settings.weekStart,
            selectedTab = tab,
            dayDetails = DayDetails(nowruz, isHoliday = true, isWeekend = false, events.toImmutableList()),
            search = CalendarSearch(),
            islamicVariant = settings.islamicVariant,
            languageCode = languageCode,
            showWeekNumbers = false,
            months = persistentListOf(),
            overview = DayDetailsCalculator.overview(nowruz, nowruz, calendars, place),
            times =
                place?.let { DayTimesState.Ready(DayDetailsCalculator.times(nowruz, it, morning)) }
                    ?: DayTimesState.NoPlace,
            sourceEvent = sourceEvent,
        )
    }
}
