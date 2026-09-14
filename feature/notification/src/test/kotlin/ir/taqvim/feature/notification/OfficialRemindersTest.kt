/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

/** T-1002 (U): reminders attached to official events resolve their next occurrence, also across a year boundary. */
class OfficialRemindersTest {
    private val nowruz = ReminderFixtures.NOWRUZ
    private val schedule = CalculatorOfficialEventSchedule(listOf(nowruz), "en")

    @Test
    fun `the next Nowruz is found across the Persian year boundary`() {
        val nowruz1405 = ReminderFixtures.jdnOf(ReminderFixtures.persian(1405, 1, 1))
        val nowruz1406 = ReminderFixtures.jdnOf(ReminderFixtures.persian(1406, 1, 1))
        val lateEsfand = ReminderFixtures.jdnOf(ReminderFixtures.persian(1404, 12, 25))

        nowruz1405 shouldBe ReminderFixtures.NOWRUZ_1405
        schedule.days(nowruz.id, lateEsfand, lateEsfand + 10) shouldBe listOf(nowruz1405)
        schedule.days(nowruz.id, nowruz1405 + 1, nowruz1405 + 400) shouldBe listOf(nowruz1406)
        schedule.days(nowruz.id, nowruz1405 + 1, nowruz1405 - 1).shouldBeEmpty()
        schedule.days(EventId("ir.holiday.unknown-1"), lateEsfand, lateEsfand + 10).shouldBeEmpty()
        schedule.title(nowruz.id) shouldBe "Nowruz"
        CalculatorOfficialEventSchedule(listOf(nowruz), "de").title(nowruz.id) shouldBe "Nowruz (fa)"
        schedule.title(EventId("ir.holiday.unknown-1")).shouldBeNull()
    }

    @Test
    fun `a reminder three days before Nowruz 1405 sounds at the all-day time and then moves to 1406`() {
        val setup =
            ReminderFixtures.setup(
                events = emptyList(),
                officials = listOf(OfficialReminder(7, nowruz.id, 3)),
                schedule = schedule,
            )

        val first = ReminderPlanner.upcoming(Instant.parse("2026-03-01T00:00:00Z"), setup).first()

        first.kind shouldBe ReminderKind.OFFICIAL
        first.occurrence shouldBe ReminderFixtures.NOWRUZ_1405
        first.daysBefore shouldBe 3
        first.title shouldBe "Nowruz"
        first.alarmSourceId shouldBe -7
        first.at shouldBe LocalDate(2026, 3, 18).atTime(9, 0).toInstant(ReminderFixtures.TEHRAN)
        ReminderPlanner.at(-7, first.at, setup) shouldBe first
        ReminderPlanner.at(7, first.at, setup).shouldBeNull()
        ReminderPlanner.upcoming(first.at, setup).first().occurrence shouldBe
            ReminderFixtures.jdnOf(ReminderFixtures.persian(1406, 1, 1))
    }

    @Test
    fun `validity, unavailable calendars and Islamic year boundaries are respected`() {
        val citation = Citation("https://example.org/source", "Synthetic source")
        val from1406 = nowruz.copy(validity = Validity(CalendarSystem.PERSIAN, 1406, null, citation))
        val nepali = nowruz.copy(id = EventId("np.holiday.new-year-1"), calendar = CalendarSystem.NEPALI)
        val islamicNewYear =
            nowruz.copy(
                id = EventId("ir.holiday.islamic-new-year-1"),
                calendar = CalendarSystem.ISLAMIC,
                category = EventCategory.RELIGIOUS,
                rule = EventRule.Fixed(month = 1, day = 1),
            )
        val calculator = CalculatorOfficialEventSchedule(listOf(from1406, nepali, islamicNewYear), "en")
        val start = ReminderFixtures.NOWRUZ_1405 - 1

        calculator.days(from1406.id, start, start + 400) shouldBe
            listOf(ReminderFixtures.jdnOf(ReminderFixtures.persian(1406, 1, 1)))
        calculator.days(nepali.id, start, start + 400).shouldBeEmpty()

        val islamic = requireNotNull(CalendarProvider.DEFAULT.calendarFor(CalendarSystem.ISLAMIC))
        val lateDhulHijjah = islamic.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1447, 12, 25))
        val newYear = calculator.days(islamicNewYear.id, lateDhulHijjah, lateDhulHijjah + 10).single()
        islamic.fromJdn(newYear) shouldBe CalendarDate(CalendarSystem.ISLAMIC, 1448, 1, 1)
        calculator.title(islamicNewYear.id).shouldNotBeNull()
    }
}
