/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** B11: the unsaved draft survives the round trip through `SavedStateHandle` text. */
class EditorDraftCodecTest {
    @Test
    fun `every form round-trips with its typed date phrase`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long()) { seed ->
                val random = Random(seed)
                val event = EditorFixtures.randomEvent(random)
                val calendar = ParseContext.DEFAULT_CALENDARS.getValue(event.calendar)
                val form = EditorForm.of(event, calendar, NumeralSystem.PERSIAN)
                val edited = form.copy(title = "${form.title} ✎", notes = "line\n\"quoted\"", colorArgb = null)
                val text = EditorCodecCases.PHRASES.random(random)

                val decoded = EditorDraftCodec.decode(EditorDraftCodec.encode(event.id, edited, text), event.id, form)

                decoded shouldBe EditorDraft(edited, text)
            }
        }

    @Test
    fun `the kept rule comes from the reopened event and only while the draft still repeats with it`() {
        val lastFriday = listOf(WeekdayNum(Weekday.FRIDAY, -1))
        val rule = RecurrenceRule(Frequency.MONTHLY, byDay = lastFriday, byMonthDay = listOf(-1))
        val start = EditorFixtures.persian(1405, 1, 1)
        val calendar = ParseContext.DEFAULT_CALENDARS.getValue(start.system)
        val repeat = RepeatForm.of(rule, start, calendar, NumeralSystem.LATIN)
        val opened = EditorFixtures.form(start = start).copy(id = 9, repeat = repeat)
        val interval = opened.copy(repeat = repeat.copy(intervalText = "2"))
        val fresh = opened.copy(repeat = RepeatForm.create(Frequency.WEEKLY, start, NumeralSystem.LATIN))

        EditorDraftCodec.decode(EditorDraftCodec.encode(9, interval, ""), 9, opened)?.form shouldBe interval
        EditorDraftCodec
            .decode(EditorDraftCodec.encode(9, fresh, ""), 9, opened)
            ?.form
            ?.repeat
            ?.keptRule
            .shouldBeNull()
    }

    @Test
    fun `drafts of another event, another version or broken text are ignored`() {
        val form = EditorFixtures.form()
        val text = EditorDraftCodec.encode(4, form, "")

        EditorDraftCodec.decode(text, 5, form).shouldBeNull()
        EditorDraftCodec.decode(text, null, form).shouldBeNull()
        EditorDraftCodec.decode(text.replace("\"v\":1", "\"v\":2"), 4, form).shouldBeNull()
        EditorDraftCodec.decode("{", 4, form).shouldBeNull()
        EditorDraftCodec.decode(text.replace("PERSIAN", "MARTIAN"), 4, form).shouldBeNull()
        EditorDraftCodec.decode(EditorDraftCodec.encode(null, form, "x"), null, form) shouldBe EditorDraft(form, "x")
    }
}

private object EditorCodecCases {
    val PHRASES = listOf("", "tomorrow", "۲۵ شهریور", "next Friday at 10")
}
