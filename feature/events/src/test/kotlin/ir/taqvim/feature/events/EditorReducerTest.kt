/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.ui.component.DateSelection
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1000 form changes: every [EditorIntent] and typed date phrases. */
class EditorReducerTest {
    private val settings = EditorFixtures.settings()
    private val persian = PersianCalendarSystem
    private val form = EditorFixtures.form()

    private fun EditorForm.reduce(vararg intents: EditorIntent): EditorForm =
        intents.fold(this) { current, intent -> EditorReducer.reduce(current, intent, settings) }

    @Test
    fun `details change title, notes, color and link`() {
        val changed =
            form.reduce(
                DetailsIntent.Title("Checkup"),
                DetailsIntent.Notes("Bring the card"),
                DetailsIntent.Color(EditorPresenter.COLORS[2]),
                DetailsIntent.Link("https://example.com"),
            )
        changed shouldBe
            form.copy(
                title = "Checkup",
                notes = "Bring the card",
                colorArgb = EditorPresenter.COLORS[2],
                sourceLink = "https://example.com",
            )
        changed.reduce(DetailsIntent.Color(null)).colorArgb.shouldBeNull()
    }

    @Test
    fun `changing the calendar keeps the same days`(): Unit =
        runBlocking {
            val calendars = Arb.element(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)
            val offsets = Arb.int(-5_000..5_000)
            checkAll(PropertyTesting.iterations, offsets, Arb.int(0..40), calendars) { offset, length, target ->
                val startJdn = EditorFixtures.TODAY + offset
                val repeating =
                    form.copy(start = persian.fromJdn(startJdn), end = persian.fromJdn(startJdn + length)).reduce(
                        SetRepeat(Frequency.YEARLY),
                        RepeatIntent.Until(DateSelection(1500, 1, 1)),
                    )
                val changed = repeating.reduce(ScheduleIntent.ChangeCalendar(target))
                val arithmetic = settings.arithmeticOf(target)

                changed.calendar shouldBe target
                arithmetic.toJdn(changed.start) shouldBe startJdn
                arithmetic.toJdn(changed.end) shouldBe startJdn + length
                val until = persian.toJdn(persian.date(1500, 1, 1))
                arithmetic.toJdn(changed.repeat.shouldNotBeNull().until) shouldBe until
            }
        }

    @Test
    fun `moving the start keeps the length and the end date can be set alone`() {
        val twoDays = form.copy(end = EditorFixtures.persian(1405, 6, 25))
        val moved = twoDays.reduce(ScheduleIntent.StartDate(DateSelection(1405, 12, 29)))

        moved.start shouldBe EditorFixtures.persian(1405, 12, 29)
        persian.toJdn(moved.end) shouldBe persian.toJdn(moved.start) + 2
        moved.reduce(ScheduleIntent.EndDate(DateSelection(1406, 2, 1))).end shouldBe EditorFixtures.persian(1406, 2, 1)
        val backwards = twoDays.reduce(ScheduleIntent.EndDate(DateSelection(1405, 6, 1)))
        backwards.reduce(ScheduleIntent.StartDate(DateSelection(1405, 6, 10))).end shouldBe
            EditorFixtures.persian(1405, 6, 10)
    }

    @Test
    fun `times keep the duration of a one-day event`() {
        val timed = form.reduce(ScheduleIntent.AllDay(false))
        timed.allDay shouldBe false

        val later = timed.reduce(ScheduleIntent.StartTime(600))
        later.startMinute shouldBe 600
        later.endMinute shouldBe 660
        timed.reduce(ScheduleIntent.StartTime(1_430)).endMinute shouldBe EditorForm.LAST_MINUTE
        timed.reduce(ScheduleIntent.EndTime(480), ScheduleIntent.StartTime(700)).endMinute shouldBe 700

        val twoDays = timed.copy(end = EditorFixtures.persian(1405, 6, 24))
        twoDays.reduce(ScheduleIntent.StartTime(1_200)).endMinute shouldBe EditorForm.DEFAULT_END_MINUTE
        twoDays.reduce(ScheduleIntent.EndTime(30)).endMinute shouldBe 30
        timed.reduce(ScheduleIntent.AllDay(true)).allDay shouldBe true
    }

    @Test
    fun `repetition is turned on, edited and turned off`() {
        form.reduce(RepeatIntent.Interval("3")) shouldBe form

        val weekly = form.reduce(SetRepeat(Frequency.WEEKLY)).repeat.shouldNotBeNull()
        weekly shouldBe RepeatForm(Frequency.WEEKLY, intervalText = "1", countText = "10", until = form.start)

        val edited =
            form.reduce(
                SetRepeat(Frequency.WEEKLY),
                RepeatIntent.Interval("2"),
                RepeatIntent.ToggleWeekday(Weekday.MONDAY),
                RepeatIntent.ToggleWeekday(Weekday.SATURDAY),
                RepeatIntent.ToggleWeekday(Weekday.MONDAY),
                RepeatIntent.Ends(RecurrenceEnd.COUNT),
                RepeatIntent.Count("7"),
                RepeatIntent.Until(DateSelection(1406, 1, 15)),
                RepeatIntent.InvalidDays(InvalidDatePolicy.LAST_DAY_OF_MONTH),
            )
        edited.repeat shouldBe
            weekly.copy(
                intervalText = "2",
                weekdays = setOf(Weekday.SATURDAY),
                end = RecurrenceEnd.COUNT,
                countText = "7",
                until = EditorFixtures.persian(1406, 1, 15),
                invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH,
            )

        val monthly = edited.reduce(SetRepeat(Frequency.MONTHLY)).repeat.shouldNotBeNull()
        monthly.frequency shouldBe Frequency.MONTHLY
        monthly.weekdays shouldBe emptySet()
        monthly.intervalText shouldBe "2"
        edited.reduce(SetRepeat(Frequency.WEEKLY)).repeat?.weekdays shouldBe setOf(Weekday.SATURDAY)
        edited.reduce(SetRepeat(null)).repeat.shouldBeNull()
    }

    @Test
    fun `reminders stay sorted and distinct`() {
        val reminded =
            form.reduce(ReminderIntent.Add(60), ReminderIntent.Add(0), ReminderIntent.Add(60), ReminderIntent.Add(15))
        reminded.reminderMinutes shouldBe listOf(0, 15, 60)
        reminded.reduce(ReminderIntent.Remove(15), ReminderIntent.Remove(999)).reminderMinutes shouldBe listOf(0, 60)
    }

    @Test
    fun `typed dates set the start, and ranges the end`() {
        val twoDays = form.copy(end = EditorFixtures.persian(1405, 6, 25))

        val written = DatePhrase.apply(twoDays, "1405/07/10", settings, EditorFixtures.TODAY).shouldNotBeNull()
        written.start shouldBe EditorFixtures.persian(1405, 7, 10)
        written.end shouldBe EditorFixtures.persian(1405, 7, 12)

        val range =
            DatePhrase.apply(form, "from 1405/07/10 to 1405/07/20", settings, EditorFixtures.TODAY).shouldNotBeNull()
        range.start shouldBe EditorFixtures.persian(1405, 7, 10)
        range.end shouldBe EditorFixtures.persian(1405, 7, 20)

        val gregorian = DatePhrase.apply(form, "2026-10-01", settings, EditorFixtures.TODAY).shouldNotBeNull()
        gregorian.calendar shouldBe CalendarSystem.PERSIAN
        gregorian.start shouldBe persian.fromJdn(LocalDate(2026, 10, 1).toJdn())

        DatePhrase.apply(form, "hello there", settings, EditorFixtures.TODAY).shouldBeNull()
        DatePhrase.apply(form, "   ", settings, EditorFixtures.TODAY).shouldBeNull()
    }
}
