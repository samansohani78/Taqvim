/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1000 validation: every rule of [EventValidator], with property tests for the numeric and date rules. */
class EventValidatorTest {
    private val calendar = PersianCalendarSystem
    private val valid = EditorFixtures.form()
    private val repeating = valid.copy(repeat = RepeatForm.create(Frequency.DAILY, valid.start, NumeralSystem.LATIN))

    private fun errors(form: EditorForm) = EventValidator.validate(form, calendar)

    @Test
    fun `a complete form has no errors`() {
        errors(valid).shouldBeEmpty()
        errors(repeating).shouldBeEmpty()
    }

    @Test
    fun `blank titles are required`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.list(Arb.element(' ', '\t', '\n'), 0..8)) { chars ->
                errors(valid.copy(title = chars.joinToString(""))) shouldBe setOf(EditorError.TITLE_REQUIRED)
            }
        }

    @Test
    fun `titles, notes and links have limits`() {
        errors(valid.copy(title = "a".repeat(EventValidator.MAX_TITLE_LENGTH) + "  ")).shouldBeEmpty()
        errors(valid.copy(title = "a".repeat(EventValidator.MAX_TITLE_LENGTH + 1))) shouldBe
            setOf(EditorError.TITLE_TOO_LONG)
        errors(valid.copy(notes = "n".repeat(EventValidator.MAX_NOTES_LENGTH))).shouldBeEmpty()
        errors(valid.copy(notes = "n".repeat(EventValidator.MAX_NOTES_LENGTH + 1))) shouldBe
            setOf(EditorError.NOTES_TOO_LONG)
        listOf("", "  ", "https://example.com", "HTTP://calendar.ut.ac.ir/Fa/", "https://a.b/c?d=e#f").forEach {
            errors(valid.copy(sourceLink = it)).shouldBeEmpty()
        }
        listOf("example.com", "ftp://example.com", "https://localhost", "https://exa mple.com", "https://").forEach {
            errors(valid.copy(sourceLink = it)) shouldBe setOf(EditorError.LINK_INVALID)
        }
    }

    @Test
    fun `an event may not end before it starts`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-400..400),
                Arb.int(0..EditorForm.LAST_MINUTE),
                Arb.int(0..EditorForm.LAST_MINUTE),
                Arb.boolean(),
            ) { days, startMinute, endMinute, allDay ->
                val end = calendar.fromJdn(calendar.toJdn(valid.start) + days)
                val form = valid.copy(end = end, allDay = allDay, startMinute = startMinute, endMinute = endMinute)
                val expected = days < 0 || (days == 0 && !allDay && endMinute < startMinute)
                (EditorError.END_BEFORE_START in errors(form)) shouldBe expected
            }
        }

    @Test
    fun `interval and count accept any digits within their range`(): Unit =
        runBlocking {
            val digits = Arb.element(NumeralSystem.LATIN, NumeralSystem.PERSIAN, NumeralSystem.EASTERN_ARABIC)
            checkAll(PropertyTesting.iterations, Arb.long(-50L..11_000L), digits) { value, numerals ->
                val text = Numerals.format(value, numerals)
                val interval = repeating.copy(repeat = repeating.repeat?.copy(intervalText = text))
                (EditorError.INTERVAL_INVALID in errors(interval)) shouldBe (value !in 1..EventValidator.MAX_INTERVAL)
                val counted = repeating.repeat?.copy(end = RecurrenceEnd.COUNT, countText = " $text ")
                (EditorError.COUNT_INVALID in errors(repeating.copy(repeat = counted))) shouldBe
                    (value !in 1..EventValidator.MAX_COUNT)
            }
        }

    @Test
    fun `non-numeric repeat fields are invalid, and the count only matters when it ends the repetition`() {
        listOf("", "abc", "1.5", "2 3").forEach { text ->
            errors(repeating.copy(repeat = repeating.repeat?.copy(intervalText = text))) shouldBe
                setOf(EditorError.INTERVAL_INVALID)
            errors(repeating.copy(repeat = repeating.repeat?.copy(countText = text))).shouldBeEmpty()
            val counted = repeating.repeat?.copy(countText = text, end = RecurrenceEnd.COUNT)
            errors(repeating.copy(repeat = counted)) shouldBe
                setOf(EditorError.COUNT_INVALID)
        }
    }

    @Test
    fun `the repetition may not end before the start`() {
        val before = calendar.fromJdn(calendar.toJdn(valid.start) - 1)
        val until = repeating.repeat?.copy(end = RecurrenceEnd.UNTIL, until = before)
        errors(repeating.copy(repeat = until)) shouldBe setOf(EditorError.UNTIL_BEFORE_START)
        errors(repeating.copy(repeat = until?.copy(until = valid.start))).shouldBeEmpty()
        errors(repeating.copy(repeat = until?.copy(end = RecurrenceEnd.NEVER))).shouldBeEmpty()
    }

    @Test
    fun `reminders are limited in number and range`() {
        errors(valid.copy(reminderMinutes = listOf(0, 5, 15, 30, EventValidator.MAX_REMINDER_MINUTES))).shouldBeEmpty()
        errors(valid.copy(reminderMinutes = listOf(0, 5, 15, 30, 60, 60))).shouldBeEmpty()
        errors(valid.copy(reminderMinutes = listOf(0, 5, 15, 30, 60, 90))) shouldBe
            setOf(EditorError.TOO_MANY_REMINDERS)
        errors(valid.copy(reminderMinutes = listOf(-1))) shouldBe setOf(EditorError.REMINDER_INVALID)
        errors(valid.copy(reminderMinutes = listOf(EventValidator.MAX_REMINDER_MINUTES + 1))) shouldBe
            setOf(EditorError.REMINDER_INVALID)
    }
}
