/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Jdn
import ir.taqvim.feature.agenda.AgendaEvent
import ir.taqvim.feature.agenda.AgendaEventKind
import ir.taqvim.feature.calendar.CalendarMessage
import ir.taqvim.feature.calendar.DayEventItem
import ir.taqvim.feature.calendar.DayEventKind
import ir.taqvim.feature.search.SearchEventKind
import ir.taqvim.feature.search.SettingsEntry
import ir.taqvim.feature.search.ToolEntry
import ir.taqvim.feature.settings.SettingsDestination
import ir.taqvim.feature.timeline.TimelineEventKind
import org.junit.jupiter.api.Test

/** ADR-0015: every feature callback leads to its destination or platform action. */
class AppRouterTest {
    private val destinations = mutableListOf<AppDestination>()
    private val urls = mutableListOf<String>()
    private val deviceEvents = mutableListOf<Long>()
    private val messages = mutableListOf<CalendarMessage>()
    private val router =
        AppRouter(
            navigate = { destinations += it },
            external =
                object : ExternalActions {
                    override fun openUrl(url: String) {
                        urls += url
                    }

                    override fun openDeviceEvent(id: Long) {
                        deviceEvents += id
                    }
                },
            showMessage = { messages += it },
        )
    private val day = Jdn(2_461_121)

    @Test
    fun `calendar callbacks open the editor, events, timeline, search, astronomy and sources`() {
        val calendar = router.calendar()

        calendar.onOpenEventEditor(day)
        calendar.onOpenEvent(DayEventItem("12", DayEventKind.PERSONAL, "Birthday", isHoliday = false))
        calendar.onOpenEvent(DayEventItem("ir.holiday.nowruz-1", DayEventKind.OFFICIAL, "Nowruz", isHoliday = true))
        calendar.onOpenEvent(DayEventItem("5", DayEventKind.DEVICE, "Meeting", isHoliday = false))
        calendar.onOpenTimeline(day)
        calendar.onOpenSearch()
        calendar.onOpenShiftWork()
        calendar.onOpenPlanetaryHours(day)
        calendar.onOpenUrl("https://calendar.ut.ac.ir/Fa/")
        calendar.onMessage(CalendarMessage.NO_UPCOMING_OCCURRENCE)

        destinations shouldBe
            listOf(
                AppDestination.EventEditor(),
                AppDestination.EventEditor(12),
                AppDestination.Calendar,
                AppDestination.Timeline(day.value),
                AppDestination.Search,
                AppDestination.Pending(PendingFeature.SHIFT_WORK),
                AppDestination.Astronomy,
            )
        deviceEvents shouldBe listOf(5L)
        urls shouldBe listOf("https://calendar.ut.ac.ir/Fa/")
        messages shouldBe listOf(CalendarMessage.NO_UPCOMING_OCCURRENCE)
    }

    @Test
    fun `year, agenda and timeline callbacks open the calendar, the editor or the device event`() {
        router.year().onOpenMonth(day)
        router.agenda().onOpenDay(day)
        router.agenda().onOpenEvent(AgendaEvent("3", AgendaEventKind.PERSONAL, "Class", isHoliday = false))
        router.agenda().onOpenEvent(AgendaEvent("2:uid", AgendaEventKind.SUBSCRIPTION, "Talk", isHoliday = false))
        router.timeline().onCreateEvent(day, 600, 660)
        router.timeline().onOpenEvent("9", TimelineEventKind.DEVICE)
        router.timeline().onOpenEvent("not-a-number", TimelineEventKind.PERSONAL)

        destinations shouldBe
            listOf(
                AppDestination.Calendar,
                AppDestination.Calendar,
                AppDestination.EventEditor(3),
                AppDestination.Calendar,
                AppDestination.EventEditor(),
                AppDestination.Calendar,
            )
        deviceEvents shouldBe listOf(9L)
    }

    @Test
    fun `search results open days, events, settings and tools`() {
        val search = router.search()

        search.onOpenDay(day)
        search.onOpenEvent(SearchEventKind.PERSONAL, "4", day)
        search.onOpenSettings(SettingsEntry.LOCATION)
        search.onOpenTool(ToolEntry.COMPASS)

        destinations shouldBe
            listOf(
                AppDestination.Calendar,
                AppDestination.EventEditor(4),
                AppDestination.Settings("LOCATION"),
                AppDestination.Compass,
            )
    }

    @Test
    fun `settings rows open their pages`() {
        SettingsDestination.entries.forEach { router.settings().onOpen(it) }

        destinations shouldBe
            listOf(
                AppDestination.LocationSettings,
                AppDestination.AthanSettings,
                AppDestination.Subscriptions,
                AppDestination.Pending(PendingFeature.WIDGETS),
            )
    }

    @Test
    fun `every settings and tool entry has a destination`() {
        SettingsEntry.entries.map(::settingsDestination) shouldBe
            listOf(
                AppDestination.Settings("LANGUAGE"),
                AppDestination.Settings("LOCATION"),
                AppDestination.Settings("MAIN_CALENDAR"),
                AppDestination.Settings("PRAYER_METHOD"),
                AppDestination.Settings("ATHAN"),
                AppDestination.Settings("PERSISTENT_NOTIFICATION"),
                AppDestination.Settings("THEME"),
                AppDestination.Settings("WIDGETS"),
                AppDestination.Pending(PendingFeature.SETTINGS),
                AppDestination.Pending(PendingFeature.SETTINGS),
                AppDestination.Pending(PendingFeature.SETTINGS),
            )
        ToolEntry.entries.map(::toolDestination) shouldBe
            listOf(
                AppDestination.Tools,
                AppDestination.Tools,
                AppDestination.Tools,
                AppDestination.Tools,
                AppDestination.Tools,
                AppDestination.Compass,
                AppDestination.Level,
                AppDestination.Astronomy,
                AppDestination.Times,
                AppDestination.Agenda,
                AppDestination.Year,
            )
    }
}
