/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFieldOrder
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.ISLAMIC
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn
import org.junit.jupiter.api.Test

/** Written dates, ambiguity ranking (U tests of T-500), tokens and contexts. */
class DateParserTest {
    private val reference: Jdn = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, 2026, 9, 13))
    private val persian = ParseContext(reference)
    private val gregorian = ParseContext(reference, GREGORIAN)

    private fun best(
        text: String,
        context: ParseContext,
    ): ParseResult = requireNotNull(DateParser.parseBest(text, context)) { "no reading of '$text'" }

    private fun day(
        year: Int,
        month: Int,
        day: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, year, month, day))

    @Test
    fun `day-month ambiguity exposes both readings ranked by the context order`() {
        val dayFirst = DateParser.parse("05/06/2026", gregorian)
        dayFirst.map { it.jdn } shouldBe listOf(day(2026, 6, 5), day(2026, 5, 6))
        dayFirst[1].confidence shouldBeLessThan dayFirst[0].confidence
        val monthFirst = DateParser.parse("05/06/2026", gregorian.copy(numericOrder = DateFieldOrder.MDY))
        monthFirst.map { it.jdn } shouldBe listOf(day(2026, 5, 6), day(2026, 6, 5))
    }

    @Test
    fun `year-first numbers are read in every plausible calendar with the preferred one first`() {
        val readings = DateParser.parse("۱۴۰۵/۰۶/۲۲", persian)
        readings.map { it.date.system } shouldBe listOf(PERSIAN, ISLAMIC)
        readings[0].jdn shouldBe reference
        DateParser.parse("1405-06-22", persian.copy(preferredCalendar = ISLAMIC)).map { it.date.system } shouldBe
            listOf(ISLAMIC, PERSIAN)
    }

    @Test
    fun `a calendar marker restricts numeric readings and extends the span`() {
        val text = "22/06/1448 ه.ق"
        val readings = DateParser.parse(text, persian)
        readings.map { it.date } shouldBe listOf(CalendarDate(ISLAMIC, 1448, 6, 22))
        readings[0].span shouldBe text.indices
        DateParser.parse("1405/06/22, x", persian)[0].span shouldBe (0 until 10)
    }

    @Test
    fun `incomplete numeric dates are not read`() {
        DateParser.parse("1405/06-22", persian).shouldBeEmpty()
        DateParser.parse("13/9/26", gregorian).shouldBeEmpty()
        DateParser.parse("1405/06", persian).shouldBeEmpty()
        DateParser.parse("", persian).shouldBeEmpty()
        DateParser.parseBest("hello, world!", persian).shouldBeNull()
    }

    @Test
    fun `month names give the calendar and a year-less date takes the nearest year`() {
        best("۲۲ شهریور ۱۴۰۵", persian).jdn shouldBe reference
        best("1405 Shahrivar 22", gregorian).jdn shouldBe reference
        best("Sunday, September 13, 2026", persian).jdn shouldBe reference
        best("Rabiʻ II 1, 1448 AH", persian).date shouldBe CalendarDate(ISLAMIC, 1448, 4, 1)
        val yearless = best("۱۵ فروردین", persian)
        yearless.date shouldBe CalendarDate(PERSIAN, 1405, 1, 15)
        yearless.confidence shouldBe AbsoluteRules.YEARLESS_CONFIDENCE
        best("اسفند ۲۵", persian).date shouldBe CalendarDate(PERSIAN, 1404, 12, 25)
    }

    @Test
    fun `every English month name is recognized`() {
        val formats = FormatTable.of(requireNotNull(LanguageTable.forCode("en")))
        listOf(PERSIAN, ISLAMIC, GREGORIAN).forEach { system ->
            formats.monthNames.getValue(system).forEachIndexed { i, name ->
                val year = if (system == GREGORIAN) 2026 else 1440
                best("1 $name $year", persian).date shouldBe CalendarDate(system, year, i + 1, 1)
            }
        }
    }

    @Test
    fun `a contradicting weekday lowers confidence and the span covers only the date`() {
        val right = best("یکشنبه ۲۲ شهریور ۱۴۰۵", persian)
        val wrong = best("دوشنبه ۲۲ شهریور ۱۴۰۵", persian)
        wrong.jdn shouldBe reference
        wrong.confidence shouldBeLessThan right.confidence
        val sentence = "جلسه ۲۲ شهریور ۱۴۰۵ است"
        best(sentence, persian).span shouldBe (sentence.indexOf("۲۲") until sentence.indexOf("۱۴۰۵") + 4)
    }

    @Test
    fun `spaced numbers follow the text order with low confidence`() {
        val spaced = best("22 6 1405", persian)
        spaced.jdn shouldBe reference
        spaced.confidence shouldBe AbsoluteRules.SPACED_CONFIDENCE
        best("2026年9月13日", gregorian).jdn shouldBe reference
        best("9 13, 2026", gregorian.copy(textOrder = DateFieldOrder.MDY)).jdn shouldBe reference
    }

    @Test
    fun `missing calendars fall back to Gregorian arithmetic`() {
        val onlyPersian = ParseContext(reference, ISLAMIC, calendars = mapOf(PERSIAN to PersianCalendarSystem))
        best("tomorrow", onlyPersian).date shouldBe CalendarDate(GREGORIAN, 2026, 9, 14)
        DateParser.parse("13 September 2026", onlyPersian).filter { it.kind == ParseKind.ABSOLUTE }.shouldBeEmpty()
    }

    @Test
    fun `tokens keep their positions and digit values`() {
        val tokens = Tokenizer.tokenize("۱۴۰۵/۰۶ ، x1234567890")
        tokens.map { it.type.name.first() }.joinToString("") shouldBe "NSNPWN"
        tokens[0].number shouldBe 1405
        tokens[3].start shouldBe 8
        tokens.last().number.shouldBeNull()
        Lexicon.number(tokens, 4).shouldBeNull()
    }

    @Test
    fun `language contexts take the numeric and long field orders`() {
        val en = ParseContext.forLanguage(requireNotNull(LanguageTable.forCode("en")), reference, GREGORIAN)
        en.numericOrder shouldBe DateFieldOrder.MDY
        en.textOrder shouldBe DateFieldOrder.MDY
        ParseContext.forLanguage(requireNotNull(LanguageTable.forCode("fa")), reference).numericOrder shouldBe
            DateFieldOrder.YMD
        ParseContext.textOrderOf("d MMMM y") shouldBe DateFieldOrder.DMY
        ParseContext.textOrderOf("MMMM y") shouldBe DateFieldOrder.DMY
        ParseContext.textOrderOf("y") shouldBe DateFieldOrder.DMY
    }
}
