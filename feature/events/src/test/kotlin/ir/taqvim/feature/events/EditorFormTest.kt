/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1000 form mapping: stored events round-trip through the form unchanged, and new forms start today. */
class EditorFormTest {
    @Test
    fun `a new event is an all-day event today in the first calendar`() {
        val form = EditorForm.new(EditorFixtures.settings("fa"), EditorFixtures.TODAY)

        form.calendar shouldBe CalendarSystem.PERSIAN
        form.start shouldBe EditorFixtures.persian(1405, 6, 23)
        form.end shouldBe form.start
        form.allDay shouldBe true
        form.timeZoneId shouldBe EditorFixtures.ZONE
        form.repeat.shouldBeNull()

        val gregorianSettings = EditorFixtures.settings(calendars = listOf(CalendarSystem.GREGORIAN))
        val gregorian = EditorForm.new(gregorianSettings, EditorFixtures.TODAY)
        gregorian.start shouldBe GregorianCalendarSystem.date(2026, 9, 14)
    }

    @Test
    fun `stored events round-trip through the form and validate cleanly`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long()) { seed ->
                val event = EditorFixtures.randomEvent(Random(seed))
                val calendar = ParseContext.DEFAULT_CALENDARS.getValue(event.calendar)
                val numerals = listOf(NumeralSystem.LATIN, NumeralSystem.PERSIAN).random(Random(seed))
                val form = EditorForm.of(event, calendar, numerals)

                form.toEvent(calendar) shouldBe event
                EventValidator.validate(form, calendar).shouldBeEmpty()
            }
        }

    @Test
    fun `rule parts the editor cannot change are kept`() {
        val rule =
            RecurrenceRule(
                frequency = Frequency.MONTHLY,
                byDay = listOf(WeekdayNum(Weekday.FRIDAY, -1), WeekdayNum(Weekday.MONDAY)),
                byMonthDay = listOf(-1),
                weekStart = Weekday.SATURDAY,
            )
        val start = EditorFixtures.persian(1405, 1, 1)
        val repeat = RepeatForm.of(rule, start, PersianCalendarSystem, NumeralSystem.LATIN)

        repeat.weekdays shouldBe setOf(Weekday.MONDAY)
        repeat.end shouldBe RecurrenceEnd.NEVER
        repeat.until shouldBe start
        val edited =
            repeat.copy(weekdays = setOf(Weekday.MONDAY, Weekday.TUESDAY), invalidDates = InvalidDatePolicy.NEXT_DAY)
        edited.toRule(PersianCalendarSystem) shouldBe
            rule.copy(
                byDay = listOf(WeekdayNum(Weekday.FRIDAY, -1), WeekdayNum(Weekday.MONDAY), WeekdayNum(Weekday.TUESDAY)),
                invalidDates = InvalidDatePolicy.NEXT_DAY,
            )
    }

    @Test
    fun `new repetitions use the language's digits and blank fields are trimmed away`() {
        val start = EditorFixtures.persian(1405, 6, 23)
        val repeat = RepeatForm.create(Frequency.WEEKLY, start, NumeralSystem.PERSIAN)

        repeat.intervalText shouldBe Numerals.format(1, NumeralSystem.PERSIAN)
        repeat.countText shouldBe Numerals.format(10, NumeralSystem.PERSIAN)
        repeat.interval shouldBe 1L
        repeat.count shouldBe 10L

        val form = EditorFixtures.form(title = "  Dentist  ").copy(notes = "  ", sourceLink = "  ", allDay = false)
        val event = form.toEvent(PersianCalendarSystem)
        event.title shouldBe "Dentist"
        event.notes shouldBe ""
        event.sourceLink.shouldBeNull()
        event.startMinute shouldBe EditorForm.DEFAULT_START_MINUTE
        event.endMinute shouldBe EditorForm.DEFAULT_END_MINUTE
    }
}
