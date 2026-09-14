/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1205…T-1209 wiring: the calendar widgets' days, holidays and events from the events repository. */
class WidgetCalendarAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")
    private val berlin = TimeZone.of("Europe/Berlin")
    private val today = LocalDate(2026, 9, 13).toJdn()
    private val noon = Instant.parse("2026-09-13T10:00:00Z")
    private val eventDay = today + 2
    private val requested = mutableListOf<JdnRange>()

    private fun events(range: JdnRange): Flow<List<DayEvents>> {
        requested += range
        return flowOf(range.map(::dayEvents))
    }

    private fun dayEvents(jdn: Jdn): DayEvents =
        DayEvents(
            jdn = jdn,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1448, 3, 1),
            hijri = null,
            official = emptyList(),
            isHoliday = jdn == eventDay,
            isWeekend = jdn.toLocalDate().dayOfWeek == DayOfWeek.FRIDAY,
            personal = if (jdn == eventDay) listOf(dentist(jdn)) else emptyList(),
            device = emptyList(),
            ics = emptyList(),
        )

    private fun dentist(jdn: Jdn) =
        PersonalOccurrence(
            eventId = 43,
            title = "Dentist",
            notes = "",
            calendarSystem = CalendarSystem.PERSIAN,
            days = jdn..jdn,
            startMinute = null,
            endMinute = null,
            timeZoneId = "Asia/Tehran",
            colorArgb = null,
            recurring = false,
        )

    private fun source(preferences: UserPreferences = persian.copy(place = tehran)) =
        PreferencesWidgetDataSource(repositoryOf(preferences), ::events, { it.name }) { berlin }

    @Test
    fun `the interactive month shows its widget's month and the bitmap month today's`(): Unit =
        runTest {
            val next = source().load(WidgetKind.MONTH_INTERACTIVE, WidgetConfig(), noon, WidgetView(monthOffset = 1))
            val mehr = next.month.shouldNotBeNull()
            mehr.title shouldBe "مهر ۱۴۰۵"
            mehr.offset shouldBe 1
            requested.last().start.toLocalDate() shouldBe LocalDate(2026, 9, 19)

            val current = source().load(WidgetKind.MONTH_BITMAP, WidgetConfig(), noon, WidgetView(monthOffset = 1))
            val shahrivar = current.month.shouldNotBeNull()
            shahrivar.title shouldBe "شهریور ۱۴۰۵"
            shahrivar.days.single { it.isToday }.dayLabel shouldBe "۲۲"
            val marked = shahrivar.days.single { it.date == eventDay.toLocalDate() }
            marked.isHoliday shouldBe true
            marked.eventCount shouldBe 1
            shahrivar.days.single { it.date == LocalDate(2026, 9, 18) }.isWeekend shouldBe true
        }

    @Test
    fun `the week strip starts on the user's week start and the schedule keeps days with events`(): Unit =
        runTest {
            val week = source().load(WidgetKind.WEEK_STRIP, WidgetConfig(), noon, WidgetView()).week
            week.map { it.date } shouldBe (12..18).map { LocalDate(2026, 9, it) }

            val schedule = source().load(WidgetKind.SCHEDULE, WidgetConfig(), noon, WidgetView()).schedule
            schedule.map { it.date } shouldBe listOf(today.toLocalDate(), eventDay.toLocalDate())
            schedule
                .last()
                .events
                .single()
                .eventId shouldBe 43
            requested.last().dayCount shouldBe 14L
        }

    @Test
    fun `the sun arc needs a place and other widgets get no calendar parts`(): Unit =
        runTest {
            val sun = source().load(WidgetKind.SUN_ARC, WidgetConfig(), noon, WidgetView()).sun.shouldNotBeNull()
            sun.sunrise shouldMatch Regex("""۰۵:[۰-۹]{2}""")
            sun.progress.shouldNotBeNull()
            source(persian).load(WidgetKind.SUN_ARC, WidgetConfig(), noon, WidgetView()).sun.shouldBeNull()

            val date = source().load(WidgetKind.DATE_1X1, WidgetConfig(), noon, WidgetView(monthOffset = 4))
            date.month.shouldBeNull()
            date.week.shouldBeEmpty()
            date.schedule.shouldBeEmpty()
            date.sun.shouldBeNull()
        }
}
