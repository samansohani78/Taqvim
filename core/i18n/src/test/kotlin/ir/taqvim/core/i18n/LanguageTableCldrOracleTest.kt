/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.DateFormatSymbols
import com.ibm.icu.text.ListFormatter
import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Every CLDR-derived field of the language table equals what ICU4J 78.3 (CLDR 48) reports for the language's
 * primary locale. Deliberate differences from raw ICU output:
 * - native names use CLDR's display name of the table code itself when CLDR has one (so `prs` is "Dari", not
 *   "Persian"), otherwise the display name of the locale's language;
 * - Persian and Islamic month names are absent where CLDR only has the generic (Gregorian-numbered) or English
 *   fallback names — those gaps are asserted in [LanguageTableTest] and listed in docs/DATA_TODO.md;
 * - numerals, calendars, prayer method and Asr convention are product defaults (ADR-0007) and not compared.
 */
class LanguageTableCldrOracleTest {
    @TestFactory
    fun `CLDR-derived fields match ICU4J`() =
        LanguageTable.languages.map { spec ->
            DynamicTest.dynamicTest(spec.code) {
                val locale = ULocale.forLanguageTag(spec.localeTag)
                withClue("${spec.code} identity") { checkIdentity(spec, locale) }
                withClue("${spec.code} week") { checkWeek(spec, locale) }
                withClue("${spec.code} formats") { checkFormats(spec, locale) }
                withClue("${spec.code} months") { checkMonths(spec, locale) }
            }
        }

    private fun checkIdentity(
        spec: LanguageSpec,
        locale: ULocale,
    ) {
        val byCode = ULocale(spec.code).getDisplayLanguage(locale)
        spec.nativeName shouldBe if (byCode == spec.code) locale.getDisplayLanguage(locale) else byCode
        spec.script shouldBe ULocale.addLikelySubtags(locale).script
        (spec.direction == TextDirection.RTL) shouldBe locale.isRightToLeft
    }

    private fun checkWeek(
        spec: LanguageSpec,
        locale: ULocale,
    ) {
        val week = Calendar.getWeekDataForRegion(ULocale.addLikelySubtags(locale).country)
        spec.weekStart shouldBe weekday(week.firstDayOfWeek)
        val weekendLength = Math.floorMod(week.weekendCease - week.weekendOnset, DAYS_PER_WEEK) + 1
        spec.weekend shouldBe (0 until weekendLength).map { weekday(week.weekendOnset) + it }.toSet()
    }

    private fun checkFormats(
        spec: LanguageSpec,
        locale: ULocale,
    ) {
        val amPm = DateFormatSymbols(locale).amPmStrings
        spec.dayPeriods shouldBe DayPeriodNames(amPm[0], amPm[1])
        val shortFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale) as? SimpleDateFormat
        spec.datePattern shouldBe DatePattern.fromCldr(shortFormat.shouldNotBeNull().toPattern())
        val and =
            ListFormatter
                .getInstance(
                    locale,
                    ListFormatter.Type.AND,
                    ListFormatter.Width.WIDE,
                ).format("{0}", "{1}")
        spec.andPattern shouldBe and.takeUnless { it == ROOT_AND_PATTERN }
    }

    private fun checkMonths(
        spec: LanguageSpec,
        locale: ULocale,
    ) {
        spec.monthNames.gregorian shouldBe months(locale, "gregorian")
        spec.monthNames.persian?.let { it shouldBe months(locale, "persian") }
        spec.monthNames.islamic?.let { it shouldBe months(locale, "islamic") }
    }

    private fun months(
        locale: ULocale,
        calendar: String,
    ): List<String> {
        val withCalendar = locale.setKeywordValue("calendar", calendar)
        val symbols = DateFormatSymbols(Calendar.getInstance(withCalendar), withCalendar)
        return symbols.getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE).toList()
    }

    /** ICU numbers weekdays 1 = Sunday … 7 = Saturday. */
    private fun weekday(icuDay: Int): Weekday = Weekday.SUNDAY + (icuDay - 1)

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val ROOT_AND_PATTERN = "{0}, {1}"
    }
}
