/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** F07: year-aware month names, the same for every calendar except the Hebrew one. */
class CalendarMonthNamesTest {
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val englishFormats = FormatTable.of(english)
    private val hindi = requireNotNull(LanguageTable.forCode("hi"))

    @Test
    fun `fixed calendars keep their flat lists`() {
        for (language in LanguageTable.languages) {
            val formats = FormatTable.of(language)
            for (system in CalendarSystem.entries - CalendarSystem.HEBREW) {
                language.monthNamesOf(system, 1405) shouldBe language.monthNames.forSystem(system)
                formats.monthNamesOf(system, 1405) shouldBe formats.monthNames[system]
                val date = CalendarDate(system, 1405, 7, 1)
                language.monthName(date) shouldBe language.monthNames.forSystem(system)?.get(6)
                formats.monthName(date) shouldBe formats.monthNames[system]?.get(6)
            }
        }
    }

    @Test
    fun `Hebrew years have 12 or 13 names`() {
        // 5784 is a leap year (cycle year 17), 5785 a common year.
        english.monthNamesOf(CalendarSystem.HEBREW, 5784).orEmpty() shouldHaveSize 13
        english.monthNamesOf(CalendarSystem.HEBREW, 5785).orEmpty() shouldHaveSize 12
        english.monthName(CalendarDate(CalendarSystem.HEBREW, 5784, 6, 1)) shouldBe "Adar I"
        english.monthName(CalendarDate(CalendarSystem.HEBREW, 5784, 7, 1)) shouldBe "Adar II"
        english.monthName(CalendarDate(CalendarSystem.HEBREW, 5785, 6, 1)) shouldBe "Adar"
        english.monthName(CalendarDate(CalendarSystem.HEBREW, 5785, 7, 1)) shouldBe "Nisan"
        englishFormats.monthName(CalendarDate(CalendarSystem.HEBREW, 5784, 13, 1)) shouldBe "Elul"
        english.monthName(CalendarDate(CalendarSystem.HEBREW, 5785, 13, 1)).shouldBeNull()
    }

    @Test
    fun `languages without CLDR Hebrew names use machine-translated ones, and unknown languages get none`() {
        hindi.monthNamesOf(CalendarSystem.HEBREW, 5784).orEmpty() shouldHaveSize 13
        FormatTable.of(hindi).monthName(CalendarDate(CalendarSystem.HEBREW, 5785, 1, 1)) shouldBe "तिश्री"
        hindi.copy(code = "xx").monthNamesOf(CalendarSystem.HEBREW, 5784).shouldBeNull()
    }
}
