/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.IcsOccurrence
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.calendar.CalendarPlace
import ir.taqvim.feature.calendar.DayEventItem
import ir.taqvim.feature.calendar.DayEventKind
import ir.taqvim.feature.times.TimesSettings
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-800 wiring: day events and search results reach the calendar screen in its own models. */
class CalendarAdaptersTest {
    private val nowruz = OfficialEvents.ALL.first { it.id.value == NOWRUZ }
    private val nowruz1405 = LocalDate(2026, 3, 21).toJdn()

    @Test
    fun `day events keep their order and are titled in the app language`() {
        val begin = Instant.parse("2026-03-21T06:30:00Z")
        val end = Instant.parse("2026-03-21T07:30:00Z")
        val days = nowruz1405..nowruz1405
        val events =
            DayEvents(
                jdn = nowruz1405,
                islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1),
                hijri = null,
                official = listOf(Occurrence(nowruz, nowruz1405, PERSIAN_NOWRUZ, isHoliday = true, year = 1405)),
                isHoliday = true,
                isWeekend = false,
                personal = listOf(birthday(days)),
                device = listOf(DeviceEvent(3, 1, "Meeting", begin, end, allDay = false, colorArgb = null, days)),
                ics = listOf(IcsOccurrence(2, "uid-1", "Talk", "", begin, end, allDay = false, days = days)),
            )

        val day = events.toCalendarDay("en")

        day.jdn shouldBe nowruz1405
        day.isHoliday shouldBe true
        day.isWeekend shouldBe false
        day.events shouldBe
            listOf(
                DayEventItem(
                    NOWRUZ,
                    DayEventKind.OFFICIAL,
                    nowruz.title.forLanguage("en"),
                    isHoliday = true,
                    source = nowruz.source,
                    citations = nowruz.citations,
                ),
                DayEventItem("7", DayEventKind.PERSONAL, "Birthday", isHoliday = false),
                DayEventItem("3", DayEventKind.DEVICE, "Meeting", isHoliday = false),
                DayEventItem("2:uid-1", DayEventKind.SUBSCRIPTION, "Talk", isHoliday = false),
            )
    }

    @Test
    fun `search results carry the next occurrence from today`(): Unit =
        runTest {
            val title = nowruz.title.forLanguage("fa")
            val beforeNowruz =
                OfficialEventSearchSource(language = { "fa" }, today = { LocalDate(2026, 3, 18).toJdn() })
            val result = beforeNowruz.search(title, limit = 5).first { it.eventId == NOWRUZ }

            result.title shouldBe title
            result.isHoliday shouldBe true
            result.nextDay shouldBe nowruz1405

            val afterNowruz =
                OfficialEventSearchSource(language = { "fa" }, today = { LocalDate(2026, 3, 22).toJdn() })
            afterNowruz.search(title, limit = 5).first { it.eventId == NOWRUZ }.nextDay shouldBe
                PersianCalendarSystem.toJdn(PersianCalendarSystem.date(1406, 1, 1))
        }

    @Test
    fun `official events carry their source and citations for the source tooltip`() {
        nowruz.citations.isNotEmpty() shouldBe true
    }

    @Test
    fun `the calendar's place follows the Times settings`(): Unit =
        runTest {
            val settings = MutableStateFlow<TimesSettings?>(null)
            val source = TimesCalendarPlaceSource { settings }

            source.place().first() shouldBe null

            val tehran =
                TimesSettings(
                    placeName = "Tehran",
                    place = Coordinates(35.69, 51.42),
                    timeZone = TimeZone.of("Asia/Tehran"),
                    prayer = PrayerSettings(),
                    language = requireNotNull(LanguageTable.forCode("fa")),
                )
            settings.value = tehran
            source.place().first() shouldBe CalendarPlace("Tehran", tehran.place, tehran.timeZone, tehran.prayer)
        }

    @Test
    fun `the secondary calendar becomes second and the primary stays first`(): Unit =
        runTest {
            val preferences = repositoryOf(UserPreferences.defaultsFor("fa"))
            val store = PreferencesCalendarDisplayStore(preferences)
            val primary =
                preferences.preferences
                    .first()
                    .calendars
                    .first()

            store.setSecondaryCalendar(CalendarSystem.GREGORIAN)
            val calendars = preferences.preferences.first().calendars
            calendars.first() shouldBe primary
            calendars[1] shouldBe CalendarSystem.GREGORIAN
            calendars.toSet().size shouldBe calendars.size
        }

    @Test
    fun `secondary calendar ordering keeps the others and adds a missing one`() {
        val persian = CalendarSystem.PERSIAN
        val islamic = CalendarSystem.ISLAMIC
        val gregorian = CalendarSystem.GREGORIAN
        withSecondary(listOf(persian, islamic, gregorian), gregorian) shouldBe listOf(persian, gregorian, islamic)
        withSecondary(listOf(persian, islamic), gregorian) shouldBe listOf(persian, gregorian, islamic)
        withSecondary(listOf(persian), persian) shouldBe listOf(persian)
        withSecondary(emptyList(), gregorian) shouldBe listOf(gregorian)
    }

    @Test
    fun `week numbers are reported as not saved until a preference exists`(): Unit =
        runTest {
            val store = PreferencesCalendarDisplayStore(repositoryOf(UserPreferences.defaultsFor("en")))

            shouldThrow<IllegalStateException> { store.setShowWeekNumbers(true) }
        }

    private companion object {
        const val NOWRUZ = "ir.holiday.nowruz-1"
        val PERSIAN_NOWRUZ = CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)

        fun birthday(days: JdnRange): PersonalOccurrence =
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
            )
    }
}
