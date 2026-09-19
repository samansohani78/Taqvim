/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn
import org.junit.jupiter.api.Test

/** Relative, anchored and range phrases; the reference day is Sunday 2026-09-13 (1405-06-22). */
class RelativePhraseTest {
    private val reference: Jdn = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, 2026, 9, 13))
    private val events = setOf("Sample Festival", "جشن نمونه")
    private val anchors = AnchorLookup { query, _ -> reference.takeIf { query in events } }
    private val persian = ParseContext(reference, anchors = anchors)
    private val english = ParseContext(reference, GREGORIAN, anchors = anchors)

    private fun best(
        text: String,
        context: ParseContext,
    ): ParseResult = requireNotNull(DateParser.parseBest(text, context)) { "no reading of '$text'" }

    private fun offsets(
        context: ParseContext,
        vararg cases: Pair<String, Int>,
    ) {
        cases.forEach { (text, offset) -> (text to best(text, context).jdn) shouldBe (text to reference + offset) }
    }

    @Test
    fun `day and week offsets`() {
        offsets(persian, "امروز" to 0, "فردا" to 1, "پس فردا" to 2, "پریروز" to -2)
        offsets(persian, "۳ روز بعد" to 3, "دو هفته قبل" to -14)
        offsets(
            english,
            "the day after tomorrow" to 2,
            "yesterday" to -1,
            "in 10 days" to 10,
            "a week ago" to -7,
            "3 weeks later" to 21,
            "next week" to 7,
            "last week" to -7,
        )
    }

    @Test
    fun `weekday phrases`() {
        offsets(english, "next Sunday" to 7, "this Sunday" to 0, "last Sunday" to -7)
        offsets(english, "next Friday" to 5, "last Friday" to -2)
        offsets(persian, "جمعه آینده" to 5, "یکشنبه گذشته" to -7, "این یکشنبه" to 0, "جمعه" to 5)
        best("جمعه", persian).confidence shouldBe 0.6
    }

    @Test
    fun `month and year offsets use the preferred calendar`() {
        best("ماه بعد", persian).date shouldBe CalendarDate(PERSIAN, 1405, 7, 22)
        best("سال گذشته", persian).date shouldBe CalendarDate(PERSIAN, 1404, 6, 22)
        best("next month", english).date shouldBe CalendarDate(GREGORIAN, 2026, 10, 13)
        best("2 years ago", english).date shouldBe CalendarDate(GREGORIAN, 2024, 9, 13)
        best("in 2 months", english).date shouldBe CalendarDate(GREGORIAN, 2026, 11, 13)
    }

    @Test
    fun `incomplete relative phrases are not read`() {
        DateParser.parse("this month", english).shouldBeEmpty()
        DateParser.parse("month", english).shouldBeEmpty()
        DateParser.parse("in the morning", english).shouldBeEmpty()
        DateParser.parse("3 foo later", english).shouldBeEmpty()
        DateParser.parse("3 days", english).shouldBeEmpty()
    }

    @Test
    fun `anchored phrases offset the event day`() {
        val text = "۲ روز قبل از جشن نمونه"
        val anchored = best(text, persian)
        anchored.kind shouldBe ParseKind.ANCHORED
        anchored.jdn shouldBe reference - 2
        anchored.span shouldBe text.indices
        best("3 weeks after Sample Festival", english).jdn shouldBe reference + 21
        best("Sample Festival", english).confidence shouldBe 0.5
    }

    @Test
    fun `an offset from an event that cannot be resolved is not read as an offset from today`() {
        // Review R02: قبل is both "ago" and the start of "before", so this used to read as three days ago.
        DateParser.parse("سه روز قبل از نوروز", ParseContext(reference)).shouldBeEmpty()
        DateParser.parse("سه روز بعد از رویداد ناشناخته", persian).shouldBeEmpty()
        DateParser.parse("3 days before Unknown Festival", english).shouldBeEmpty()
        offsets(persian, "سه روز قبل" to -3, "سه روز بعد" to 3, "۲ هفته پیش" to -14)
    }

    @Test
    fun `anchored phrases need a lookup, a unit, a direction and a known event`() {
        DateParser.parse("۲ روز قبل از جشن نمونه", ParseContext(reference)).shouldBeEmpty()
        DateParser.parse("2 days before Unknown Festival", english).shouldBeEmpty()
        DateParser.parse("2 foo before Sample Festival", english).map { it.span } shouldBe listOf(13..27)
        DateParser.parse("2 days around Sample Festival", english).map { it.span } shouldBe listOf(14..28)
    }

    @Test
    fun `ranges join two dates`() {
        val text = "از ۱ مهر ۱۴۰۵ تا ۱۵ مهر ۱۴۰۵"
        val range = best(text, persian)
        range.kind shouldBe ParseKind.RANGE
        range.date shouldBe CalendarDate(PERSIAN, 1405, 7, 1)
        range.end?.date shouldBe CalendarDate(PERSIAN, 1405, 7, 15)
        range.span shouldBe text.indices
        best("September 1, 2026 - September 5, 2026", english).end?.jdn shouldBe reference - 8
        best("between 1 September 2026 and 5 September 2026", english).span.first shouldBe 0
        best("from today until next Friday", english).end?.jdn shouldBe reference + 5
    }

    @Test
    fun `shared-month ranges take the month and year of the second date`() {
        val range = best("۱۰ تا ۲۰ شهریور ۱۴۰۵", persian)
        range.date shouldBe CalendarDate(PERSIAN, 1405, 6, 10)
        range.end?.date shouldBe CalendarDate(PERSIAN, 1405, 6, 20)
        best("از ۱۰ تا ۲۰ شهریور ۱۴۰۵", persian).span.first shouldBe 0
    }

    @Test
    fun `backward or impossible ranges are not read`() {
        DateParser.parse("۱۵ مهر ۱۴۰۵ تا ۱ مهر ۱۴۰۵", persian).filter { it.kind == ParseKind.RANGE }.shouldBeEmpty()
        DateParser.parse("۲۰ تا ۱۰ شهریور ۱۴۰۵", persian).filter { it.kind == ParseKind.RANGE }.shouldBeEmpty()
        DateParser.parse("۳۱ تا ۲۰ مهر ۱۴۰۵", persian).filter { it.kind == ParseKind.RANGE }.shouldBeEmpty()
        DateParser.parse("۱۴۰۵ تا ۲۰ مهر ۱۴۰۵", persian).filter { it.kind == ParseKind.RANGE }.shouldBeEmpty()
    }
}
