/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.text.DateFormatSymbols
import com.ibm.icu.util.HebrewCalendar
import com.ibm.icu.util.ULocale
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/** F07: CLDR Hebrew month names, re-checked against ICU4J on every run (the T-200 oracle approach). */
class HebrewMonthNamesTest {
    private val languages = LanguageTable.languages.map { it.code }

    private fun icuNames(tag: String): List<String> {
        val locale = ULocale.forLanguageTag(tag)
        return DateFormatSymbols(HebrewCalendar(locale), locale)
            .getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE)
            .toList()
    }

    @Test
    fun `English names follow the calendar's numbering in common and leap years`() {
        HebrewMonthNames.months("en", leap = false) shouldContainExactly
            listOf(
                "Tishri",
                "Heshvan",
                "Kislev",
                "Tevet",
                "Shevat",
                "Adar",
                "Nisan",
                "Iyar",
                "Sivan",
                "Tamuz",
                "Av",
                "Elul",
            )
        HebrewMonthNames.months("en", leap = true) shouldContainExactly
            listOf(
                "Tishri",
                "Heshvan",
                "Kislev",
                "Tevet",
                "Shevat",
                "Adar I",
                "Adar II",
                "Nisan",
                "Iyar",
                "Sivan",
                "Tamuz",
                "Av",
                "Elul",
            )
    }

    @Test
    fun `every included language matches CLDR through ICU4J`() {
        val included = languages.filterNot { it in HebrewMonthNames.omittedLanguages }
        included.size shouldBe languages.size - HebrewMonthNames.omittedLanguages.size
        included.forEach { code ->
            val tag = HebrewMonthNames.locale(code)
            tag shouldNotBe null
            val cldr = icuNames(requireNotNull(tag))
            HebrewMonthNames.months(code, leap = true) shouldContainExactly
                listOf(0, 1, 2, 3, 4, 5, 13, 7, 8, 9, 10, 11, 12).map(cldr::get)
            HebrewMonthNames.months(code, leap = false)?.size shouldBe 12
        }
    }

    @Test
    fun `omitted languages have only fallback or generic names in CLDR`() {
        val root = icuNames("und")
        HebrewMonthNames.omittedLanguages shouldBe
            setOf("ps", "ckb", "kmr", "az", "ne", "hi", "tg", "uz", "id", "ms", "zh")
        HebrewMonthNames.omittedLanguages.forEach { code ->
            HebrewMonthNames.locale(code) shouldBe null
            val tag = requireNotNull(LanguageTable.forCode(code)).localeTag
            if (code != "zh") icuNames(tag) shouldBe root
        }
        icuNames("zh-Hans").first() shouldBe "一月"
    }

    @Test
    fun `month numbers resolve by the 19-year leap cycle`() {
        listOf(3, 6, 8, 11, 14, 17, 19).forEach { position ->
            HebrewMonthNames.isLeapYear(19 * 300 + position) shouldBe
                true
        }
        (1..19).count { HebrewMonthNames.isLeapYear(19 * 300 + it) } shouldBe 7
        HebrewMonthNames.name("en", 5784, 7) shouldBe "Adar II"
        HebrewMonthNames.name("en", 5785, 7) shouldBe "Nisan"
        HebrewMonthNames.name("en", 5785, 13) shouldBe null
        HebrewMonthNames.name("en", 5784, 13) shouldBe "Elul"
        HebrewMonthNames.name("hi", 5784, 1) shouldBe "तिश्री"
    }

    @Test
    fun `machine-translated names fill exactly the CLDR gaps (DT-037)`() {
        languages.forEach { code ->
            HebrewMonthNames.isMachineTranslated(code) shouldBe (code in HebrewMonthNames.omittedLanguages)
            val common = requireNotNull(HebrewMonthNames.months(code, leap = false))
            val leap = requireNotNull(HebrewMonthNames.months(code, leap = true))
            common.size shouldBe 12
            leap.size shouldBe 13
            (common + leap).forEach { it.isNotBlank() shouldBe true }
            leap.distinct().size shouldBe 13
        }
        HebrewMonthNames.isMachineTranslated("en") shouldBe false
        HebrewMonthNames.isMachineTranslated("xx") shouldBe false
        HebrewMonthNames.months("hi", leap = true)?.subList(5, 7) shouldContainExactly
            listOf("अदार प्रथम", "अदार द्वितीय")
    }

    @Test
    fun `every language names every month of every Hebrew year of SH 1380 to 1480`() {
        // SH 1380–1480 spans AM 5761–5862.
        (5761..5862).forEach { year ->
            val months = if (HebrewMonthNames.isLeapYear(year)) 13 else 12
            languages.forEach { code ->
                (1..months).forEach { month -> HebrewMonthNames.name(code, year, month).isNullOrBlank() shouldBe false }
            }
        }
    }

    @Test
    fun `unknown languages have no names`() {
        HebrewMonthNames.months("xx", leap = true) shouldBe null
        HebrewMonthNames.locale("xx") shouldBe null
    }
}
