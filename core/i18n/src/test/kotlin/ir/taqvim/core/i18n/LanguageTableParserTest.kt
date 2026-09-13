/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class LanguageTableParserTest {
    private val months = (1..12).joinToString("|") { "m$it" }

    private val valid =
        mapOf(
            "languages" to "xx",
            "xx.locale" to "xx-XX",
            "xx.nativeName" to "Xx",
            "xx.script" to "Latn",
            "xx.direction" to "LTR",
            "xx.numerals" to "LATIN",
            "xx.calendars" to "GREGORIAN,ISLAMIC",
            "xx.weekStart" to "MONDAY",
            "xx.weekend" to "SATURDAY,SUNDAY",
            "xx.prayerMethod" to "MWL",
            "xx.asrJuristic" to "HANAFI",
            "xx.datePattern" to "dd.MM.y",
            "xx.am" to "am",
            "xx.pm" to "pm",
            "xx.months.gregorian" to months,
            "xx.months.nepali" to months,
        )

    private fun failure(entries: Map<String, String>): String =
        shouldThrow<IllegalArgumentException> { LanguageTableParser.parse(entries) }.message.orEmpty()

    @Test
    fun `parses a complete entry with optional fields absent`() {
        val spec = LanguageTableParser.parse(valid).single()

        spec.monthNames.nepali?.last() shouldBe "m12"
        spec.monthNames.persian shouldBe null
        spec.andPattern shouldBe null
        spec.datePattern shouldBe DatePattern(DateFieldOrder.DMY, '.', zeroPad = true)
    }

    @Test
    fun `missing and malformed entries are rejected with the language and field`() {
        failure(valid - "languages") shouldContain "'languages'"
        failure(valid - "xx.script") shouldContain "language 'xx' has no 'script'"
        failure(valid - "xx.months.gregorian") shouldContain "'months.gregorian'"
        failure(valid + ("xx.weekStart" to "FUNDAY")) shouldContain "'FUNDAY' is not a Weekday"
        failure(valid + ("xx.months.persian" to "a|b")) shouldContain "expected 12 month names"
        failure(valid + ("xx.calendars" to "PERSIAN,PERSIAN")) shouldContain "invalid calendars"
        failure(valid + ("xx.andPattern" to "{0} & ")) shouldContain "bad and-pattern"
        failure(valid + ("xx.datePattern" to "MM/yy")) shouldContain "must contain y, M and d"
        failure(valid + ("xx.datePattern" to "yMd")) shouldContain "has no separator"
    }

    @Test
    fun `weekend and calendars must not be empty`() {
        shouldThrow<IllegalArgumentException> {
            LanguageTableParser.parse(valid).single().copy(weekend = emptySet())
        }.message shouldContain "weekend must not be empty"
        shouldThrow<IllegalArgumentException> {
            LanguageTableParser.parse(valid).single().copy(calendars = emptyList())
        }.message shouldContain "invalid calendars"
    }

    @Test
    fun `CLDR short date patterns map to field order, separator and padding`() {
        DatePattern.fromCldr("y/M/d") shouldBe DatePattern(DateFieldOrder.YMD, '/', zeroPad = false)
        DatePattern.fromCldr("M/d/yy") shouldBe DatePattern(DateFieldOrder.MDY, '/', zeroPad = false)
        DatePattern.fromCldr("y-MM-dd") shouldBe DatePattern(DateFieldOrder.YMD, '-', zeroPad = true)
        DatePattern.fromCldr("d. M. yy") shouldBe DatePattern(DateFieldOrder.DMY, '.', zeroPad = false)
        DatePattern.fromCldr("d'/'M'/'y") shouldBe DatePattern(DateFieldOrder.DMY, '/', zeroPad = false)
    }
}
