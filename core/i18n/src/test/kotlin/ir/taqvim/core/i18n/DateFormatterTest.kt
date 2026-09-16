/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.text.DateFormat
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.TimeZone
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import java.text.FieldPosition
import java.util.Properties
import kotlin.random.Random
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DateFormatterTest {
    private fun language(code: String) = requireNotNull(LanguageTable.forCode(code))

    private val sunday = Weekday.SUNDAY

    @Test
    fun `numeric dates follow the language order, separator and padding`() {
        val september13 = CalendarDate(CalendarSystem.GREGORIAN, 2026, 9, 13)
        val march5 = CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 5)

        DateFormatter.format(september13, sunday, language("en"), DateStyle.NUMERIC) shouldBe "9/13/2026"
        DateFormatter.format(march5, sunday, language("de"), DateStyle.NUMERIC) shouldBe "05.03.2026"
        DateFormatter.format(march5, sunday, language("ja"), DateStyle.NUMERIC) shouldBe "2026/03/05"
        DateFormatter.format(september13, sunday, language("fa"), DateStyle.NUMERIC) shouldBe
            Numerals.localizeDigits("2026/9/13", NumeralSystem.PERSIAN)
    }

    @Test
    fun `ISO dates use Latin digits unless other numerals are requested`() {
        val date = CalendarDate(CalendarSystem.PERSIAN, 1405, 6, 22)

        DateFormatter.format(date, sunday, language("fa"), DateStyle.ISO) shouldBe "1405-06-22"
        DateFormatter.format(date, sunday, language("fa"), DateStyle.ISO, NumeralSystem.PERSIAN) shouldBe
            Numerals.localizeDigits("1405-06-22", NumeralSystem.PERSIAN)
        DateFormatter.format(
            CalendarDate(CalendarSystem.GREGORIAN, -44, 3, 15),
            sunday,
            language("en"),
            DateStyle.ISO,
        ) shouldBe
            "-0044-03-15"
    }

    @Test
    fun `long dates degrade without month names or calendar abbreviations`() {
        val russian = language("ru")
        val islamic =
            DateFormatter.format(
                CalendarDate(CalendarSystem.ISLAMIC, 1448, 4, 1),
                sunday,
                russian,
                DateStyle.LONG,
            )
        islamic shouldNotContain "AH"
        // CLDR separates the year and the abbreviation with a no-break space.
        islamic.replace('\u00A0', ' ').replace('\u202F', ' ').endsWith("1448 г.") shouldBe true

        // Sunday 13 September 2026 is Bhadra 28, 2083 (ADR-0030).
        val bhadra28 = CalendarDate(CalendarSystem.NEPALI, 2083, 5, 28)
        DateFormatter.format(bhadra28, sunday, language("ne"), DateStyle.LONG) shouldBe "वि.सं. २०८३ भदौ २८, आइतबार"
        DateFormatter.format(bhadra28, sunday, language("en"), DateStyle.LONG) shouldBe "Sunday, Bhadra 28, 2083"
        DateFormatter.format(bhadra28, sunday, language("ne"), DateStyle.NUMERIC) shouldBe
            Numerals.localizeDigits("2083/5/28", NumeralSystem.DEVANAGARI)
    }

    @Test
    fun `Hebrew dates name the month by the year's leap status (F07)`() {
        // AM 5784 is a leap year (month 7 = Adar II), AM 5785 a common year (month 7 = Nisan).
        val adar2 = CalendarDate(CalendarSystem.HEBREW, 5784, 7, 14)
        val nisan = CalendarDate(CalendarSystem.HEBREW, 5785, 7, 14)
        val tishri = CalendarDate(CalendarSystem.HEBREW, 5785, 1, 1)
        DateFormatter.format(adar2, sunday, language("en"), DateStyle.LONG) shouldBe "Sunday, Adar II 14, 5784"
        DateFormatter.format(nisan, sunday, language("en"), DateStyle.LONG) shouldBe "Sunday, Nisan 14, 5785"
        DateFormatter.format(tishri, sunday, language("de"), DateStyle.LONG).contains("Tischri") shouldBe true
        // `hi` has no CLDR Hebrew names, so the month is written as a number.
        DateFormatter.format(tishri, sunday, language("hi"), DateStyle.LONG).contains("Tishri") shouldBe false
    }

    @Test
    fun `the pattern engine handles padding, two-digit years, quotes and unknown letters`() {
        val formats = FormatTable.of(language("en"))
        val date = CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 5)

        fun render(pattern: String) = DateFormatter.formatPattern(pattern, date, sunday, formats, NumeralSystem.LATIN)
        render("dd/MM/yy") shouldBe "05/03/26"
        render("EEEE d MMM y G") shouldBe "Sunday 5 March 2026 AD"
        render("d 'of' LLLL, ''y''") shouldBe "5 of March, '2026'"
        render("y 'open") shouldBe "2026 open"
        shouldThrow<IllegalArgumentException> { render("d Q") }
    }

    @Test
    fun `Persian long dates in fa and prs use the language's CLDR Gregorian pattern (ADR-0014)`() {
        FormatTable.PRODUCT_DATE_PATTERNS.keys shouldBe
            setOf("fa" to CalendarSystem.PERSIAN, "prs" to CalendarSystem.PERSIAN)
        val generated = FormatTableParser.parse(loadGeneratedFormats(), listOf("fa", "prs"))
        FormatTable.PRODUCT_DATE_PATTERNS.forEach { (key, pattern) ->
            val (code, calendar) = key
            val cldr = generated.getValue(code)
            pattern shouldBe cldr.datePatterns.getValue(CalendarSystem.GREGORIAN)
            cldr.datePatterns.getValue(calendar) shouldBe "y MMMM d, EEEE"
            FormatTable.formats
                .getValue(code)
                .datePatterns
                .getValue(calendar) shouldBe pattern
        }

        val date = CalendarDate(CalendarSystem.PERSIAN, 1405, 6, 22)
        DateFormatter.format(date, sunday, language("fa"), DateStyle.LONG) shouldBe
            Numerals.localizeDigits("یکشنبه 22 شهریور 1405", NumeralSystem.PERSIAN)
    }

    /** The generated CLDR table as stored, before product overrides. */
    private fun loadGeneratedFormats(): Map<String, String> {
        val stream = requireNotNull(FormatTable::class.java.getResourceAsStream("formats.properties"))
        val properties = Properties()
        stream.reader(Charsets.UTF_8).use { properties.load(it) }
        return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
    }

    @TestFactory
    fun `long dates match ICU4J wherever the language has complete data`(): List<DynamicTest> =
        LanguageTable.languages.flatMap { language ->
            ORACLE_CALENDARS
                .filter { (calendar, _) -> (language.code to calendar) !in FormatTable.PRODUCT_DATE_PATTERNS }
                .filter { (calendar, _) -> hasCompleteData(FormatTable.of(language), calendar) }
                .map { (calendar, years) ->
                    DynamicTest.dynamicTest("${language.code} $calendar") { compareWithIcu(language, calendar, years) }
                }
        }

    private fun hasCompleteData(
        formats: LanguageFormats,
        calendar: CalendarSystem,
    ): Boolean =
        calendar in formats.monthNames && ('G' !in formats.datePatterns.getValue(calendar) || calendar in formats.eras)

    private fun compareWithIcu(
        language: LanguageSpec,
        calendar: CalendarSystem,
        years: IntRange,
    ) {
        val locale = language.icuLocale.setKeywordValue("calendar", calendar.name.lowercase())
        val icu = Calendar.getInstance(TimeZone.GMT_ZONE, locale)
        val format = DateFormat.getDateInstance(icu, DateFormat.FULL, locale)
        val random = Random(calendar.ordinal * 100 + language.code.hashCode())
        val mismatches =
            (1..DATES_PER_CALENDAR).mapNotNull {
                val date =
                    CalendarDate(
                        calendar,
                        random.nextInt(years.first, years.last),
                        random.nextInt(1, 13),
                        random.nextInt(1, 29),
                    )
                icu.clear()
                icu.set(Calendar.EXTENDED_YEAR, date.year)
                icu.set(Calendar.MONTH, date.month - 1)
                icu.set(Calendar.DAY_OF_MONTH, date.day)
                val weekday =
                    Weekday.entries[
                        Math.floorMod(
                            icu.get(Calendar.DAY_OF_WEEK) + ISO_SHIFT,
                            Weekday.entries.size,
                        ),
                    ]
                val expected = normalizeForOracle(format.format(icu, StringBuffer(), FieldPosition(0)).toString())
                val actual = normalizeForOracle(DateFormatter.format(date, weekday, language, DateStyle.LONG))
                "$date: expected '$expected' but was '$actual'".takeIf { expected != actual }
            }
        withClue(mismatches.take(3).joinToString("\n")) { mismatches.shouldBeEmpty() }
    }

    private companion object {
        const val DATES_PER_CALENDAR = 40
        const val ISO_SHIFT = 5
        val ORACLE_CALENDARS =
            listOf(
                CalendarSystem.GREGORIAN to 1900..2100,
                CalendarSystem.PERSIAN to 1300..1500,
                CalendarSystem.ISLAMIC to 1350..1550,
            )
    }
}
