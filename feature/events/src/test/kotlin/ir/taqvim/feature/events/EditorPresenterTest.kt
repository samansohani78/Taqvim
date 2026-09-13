/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.Test

/** T-1000 presentation: states, localized dates, times, weekdays, reminders and picker data. */
class EditorPresenterTest {
    private val form = EditorFixtures.form()

    private fun editing(
        session: EditorSession.Editing,
        settings: EditorSettings = EditorFixtures.settings(),
    ): EditorContent.Editing = EditorPresenter.present(session, settings).content.shouldBeInstanceOf()

    @Test
    fun `sessions map to their contents`() {
        val settings = EditorFixtures.settings()
        EditorPresenter.present(EditorSession.Loading, settings).content shouldBe EditorContent.Loading
        EditorPresenter.present(EditorSession.NotFound, settings).content shouldBe EditorContent.NotFound
        EditorPresenter.present(EditorSession.Finished(EditorOutcome.SAVED), settings).content shouldBe
            EditorContent.Finished(EditorOutcome.SAVED)
        EditorPresenter.present(EditorSession.Editing(form, form), null).content shouldBe EditorContent.Loading
    }

    @Test
    fun `errors appear only after a save attempt and changes are tracked`() {
        val blank = form.copy(title = "")
        val quiet = editing(EditorSession.Editing(blank, form))
        quiet.errors.shouldBeEmpty()
        quiet.hasChanges shouldBe true
        quiet.isNew shouldBe true

        val shown = editing(EditorSession.Editing(blank, form, showErrors = true, busy = true, storeFailed = true))
        shown.errors shouldBe setOf(EditorError.TITLE_REQUIRED)
        shown.busy shouldBe true
        shown.storeFailed shouldBe true

        val storedForm = form.copy(id = 4)
        val stored = editing(EditorSession.Editing(storedForm, storedForm, dateText = "x", dateTextUnrecognized = true))
        stored.isNew shouldBe false
        stored.hasChanges shouldBe false
        stored.dateText shouldBe "x"
        stored.dateTextUnrecognized shouldBe true
    }

    @Test
    fun `dates and times are written in the app language`() {
        val fa = EditorFixtures.language("fa")
        val timed =
            form.copy(allDay = false, startMinute = 545, endMinute = 1_439, end = EditorFixtures.persian(1405, 6, 24))
        val display = editing(EditorSession.Editing(timed, timed), EditorFixtures.settings("fa")).display

        display.startDate shouldBe DateFormatter.format(timed.start, Weekday.MONDAY, fa, DateStyle.LONG)
        display.endDate shouldBe DateFormatter.format(timed.end, Weekday.TUESDAY, fa, DateStyle.LONG)
        display.startTime shouldBe Numerals.localizeDigits("09:05", NumeralSystem.PERSIAN)
        display.endTime shouldBe Numerals.localizeDigits("23:59", NumeralSystem.PERSIAN)
        display.untilDate.shouldBeNull()
        display.colors shouldBe EditorPresenter.COLORS
        display.calendars shouldBe EditorFixtures.CALENDARS

        val gregorianOnly = EditorFixtures.settings(calendars = listOf(CalendarSystem.GREGORIAN))
        editing(EditorSession.Editing(form, form), gregorianOnly).display.calendars shouldBe
            listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN)
    }

    @Test
    fun `weekdays start on the language's first day and show the selection`() {
        val fa = EditorFixtures.language("fa")
        val repeat =
            RepeatForm
                .create(Frequency.WEEKLY, form.start, NumeralSystem.PERSIAN)
                .copy(weekdays = setOf(Weekday.FRIDAY))
        val weekly = form.copy(repeat = repeat)
        val display = editing(EditorSession.Editing(weekly, weekly), EditorFixtures.settings("fa")).display
        val names = FormatTable.of(fa).weekdays.getValue(CalendarSystem.PERSIAN)

        display.weekdays.first().weekday shouldBe fa.weekStart
        display.weekdays.map { it.weekday }.toSet() shouldBe Weekday.entries.toSet()
        display.weekdays.forEach { option ->
            option.label shouldBe names[option.weekday.ordinal]
            option.selected shouldBe (option.weekday == Weekday.FRIDAY)
        }
        display.untilDate shouldBe DateFormatter.format(form.start, Weekday.MONDAY, fa, DateStyle.LONG)
    }

    @Test
    fun `reminders are described and presets exclude those already added`() {
        val en = EditorFixtures.language("en")
        val reminded = form.copy(reminderMinutes = listOf(0, 15, 100))
        val display = editing(EditorSession.Editing(reminded, reminded)).display

        display.reminders shouldBe
            listOf(
                ReminderOption(0, ""),
                ReminderOption(15, DurationFormatter.format(15.minutes, en).orEmpty()),
                ReminderOption(100, DurationFormatter.format(100.minutes, en).orEmpty()),
            )
        display.reminderChoices.map { it.minutes } shouldBe listOf(5, 30, 60, 1_440, 10_080)
    }

    @Test
    fun `the picker follows the form's calendar and falls back to month numbers`() {
        val fa = EditorFixtures.language("fa")
        val picker = editing(EditorSession.Editing(form, form), EditorFixtures.settings("fa")).display.picker

        picker.monthNames shouldBe FormatTable.of(fa).monthNames.getValue(CalendarSystem.PERSIAN)
        picker.daysInMonth(1405, 12) shouldBe PersianCalendarSystem.monthLength(1405, 12)
        picker.formatNumber(25) shouldBe Numerals.format(25, NumeralSystem.PERSIAN)
        picker.formatTwoDigits(7) shouldBe Numerals.localizeDigits("07", NumeralSystem.PERSIAN)
        picker.years shouldBe 1305..1505

        val zh = EditorFixtures.language("zh")
        val numbered = editing(EditorSession.Editing(form, form), EditorFixtures.settings("zh")).display.picker
        FormatTable.of(zh).monthNames[CalendarSystem.PERSIAN].shouldBeNull()
        numbered.monthNames shouldBe (1..12).map { Numerals.format(it.toLong(), zh.numerals) }
    }
}
