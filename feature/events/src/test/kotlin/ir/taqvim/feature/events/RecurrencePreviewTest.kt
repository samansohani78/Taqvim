/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarSystem
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** Review F02: the next occurrences shown while editing a repeating event. */
class RecurrencePreviewTest {
    private val persian = PersianCalendarSystem
    private val gregorian = GregorianCalendarSystem

    private fun repeating(
        form: EditorForm,
        frequency: Frequency,
        invalidDates: InvalidDatePolicy = InvalidDatePolicy.SKIP,
    ): EditorForm =
        form.copy(
            repeat = RepeatForm.create(frequency, form.start, NumeralSystem.LATIN).copy(invalidDates = invalidDates),
        )

    @Test
    fun `a weekly series lists the next ten weeks from today`() {
        val form = repeating(EditorFixtures.form(), Frequency.WEEKLY)
        val slots = RecurrencePreview.slots(form, persian, EditorFixtures.TODAY, EditorFixtures.ZONE)
        slots.map { it.day } shouldBe List(RecurrencePreview.SIZE) { EditorFixtures.TODAY + it * 7L }
        slots.map { it.minute }.toSet() shouldBe setOf(null)
    }

    @Test
    fun `an old series starts at today, not at its first occurrence`() {
        val form = repeating(EditorFixtures.form(start = EditorFixtures.persian(1400, 1, 1)), Frequency.DAILY)
        val slots = RecurrencePreview.slots(form, persian, EditorFixtures.TODAY, EditorFixtures.ZONE)
        slots.first().day shouldBe EditorFixtures.TODAY
        slots shouldHaveSize RecurrencePreview.SIZE
    }

    @Test
    fun `a yearly series on the Persian leap day skips common years or moves to the next day`() {
        val leapDay = EditorFixtures.form(start = EditorFixtures.persian(1403, 12, 30))
        val skipped =
            RecurrencePreview.slots(
                repeating(leapDay, Frequency.YEARLY),
                persian,
                EditorFixtures.TODAY,
                "UTC",
            )
        skipped shouldHaveSize RecurrencePreview.SIZE
        skipped.map { persian.fromJdn(it.day) }.forEach { date ->
            (date.month to date.day) shouldBe (12 to 30)
            persian.isLeapYear(date.year) shouldBe true
        }
        val moved =
            RecurrencePreview.slots(
                repeating(leapDay, Frequency.YEARLY, InvalidDatePolicy.NEXT_DAY),
                persian,
                EditorFixtures.TODAY,
                "UTC",
            )
        moved shouldHaveSize RecurrencePreview.SIZE
        moved.map { it.day }.zipWithNext().forEach { (a, b) -> (b > a) shouldBe true }
        moved.map { persian.fromJdn(it.day) }.forEach { date ->
            // A common year's missing 30 Esfand moves to 1 Farvardin of the next year.
            if (date.month == 12) {
                date.day shouldBe 30
                persian.isLeapYear(date.year) shouldBe true
            } else {
                (date.month to date.day) shouldBe (1 to 1)
                persian.isLeapYear(date.year - 1) shouldBe false
            }
        }
    }

    @Test
    fun `a timed series follows the viewer's zone across a DST change`() {
        // 09:00 in Berlin every Monday from 19 October 2026; Berlin leaves summer time on 25 October.
        val start = gregorian.date(2026, 10, 19)
        val form =
            repeating(
                EditorFixtures.form().copy(
                    calendar = CalendarSystem.GREGORIAN,
                    start = start,
                    end = start,
                    allDay = false,
                    startMinute = 9 * 60,
                    endMinute = 10 * 60,
                    timeZoneId = "Europe/Berlin",
                ),
                Frequency.WEEKLY,
            )
        val slots = RecurrencePreview.slots(form, gregorian, LocalDate(2026, 10, 1).toJdn(), "Asia/Tehran")
        slots.take(2).map { it.minute } shouldBe listOf(10 * 60 + 30, 11 * 60 + 30)
        slots.first().day shouldBe LocalDate(2026, 10, 19).toJdn()
        val berlin = RecurrencePreview.slots(form, gregorian, LocalDate(2026, 10, 1).toJdn(), "Europe/Berlin")
        berlin.map { it.minute }.toSet() shouldBe setOf(9 * 60)
    }

    @Test
    fun `a late event in a zone behind the viewer moves to the next day`() {
        val start = gregorian.date(2026, 9, 14)
        val form =
            repeating(
                EditorFixtures.form().copy(
                    calendar = CalendarSystem.GREGORIAN,
                    start = start,
                    end = start,
                    allDay = false,
                    startMinute = 22 * 60,
                    endMinute = 23 * 60,
                    timeZoneId = "America/Los_Angeles",
                ),
                Frequency.DAILY,
            )
        val slot = RecurrencePreview.slots(form, gregorian, EditorFixtures.TODAY, "Asia/Tehran").first()
        slot shouldBe PreviewSlot(EditorFixtures.TODAY + 1, 8 * 60 + 30)
    }

    @Test
    fun `nothing is previewed for a one-off event or an invalid repetition`() {
        val form = EditorFixtures.form()
        RecurrencePreview.slots(form, persian, EditorFixtures.TODAY, EditorFixtures.ZONE).shouldBeEmpty()
        val invalid = repeating(form, Frequency.DAILY).let { it.copy(repeat = it.repeat?.copy(intervalText = "0")) }
        RecurrencePreview.slots(invalid, persian, EditorFixtures.TODAY, EditorFixtures.ZONE).shouldBeEmpty()
    }

    @Test
    fun `the presenter localizes the preview`() {
        val form = repeating(EditorFixtures.form(), Frequency.WEEKLY).copy(allDay = false, startMinute = 9 * 60 + 5)
        val content: EditorContent.Editing =
            EditorPresenter
                .present(EditorSession.Editing(form, form), EditorFixtures.settings("fa"), EditorFixtures.TODAY)
                .content
                .shouldBeInstanceOf()
        content.display.preview shouldHaveSize RecurrencePreview.SIZE
        content.display.preview
            .first()
            .time shouldBe "۰۹:۰۵"
        content.display.preview
            .first()
            .date shouldBe content.display.startDate
        val withoutToday: EditorContent.Editing =
            EditorPresenter
                .present(EditorSession.Editing(form, form), EditorFixtures.settings())
                .content
                .shouldBeInstanceOf()
        withoutToday.display.preview
            .first()
            .date shouldBe withoutToday.display.startDate
    }
}
