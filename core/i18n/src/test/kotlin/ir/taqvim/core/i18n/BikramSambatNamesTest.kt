/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** T-105: Bikram Sambat names from the official National Panchang and the Government of Nepal's English spellings. */
class BikramSambatNamesTest {
    @Test
    fun `Nepali names are the Panchang's and other languages use the official Latin spellings`() {
        BikramSambatNames.months("ne") shouldBe
            listOf("वैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज", "कात्तिक", "मङ्सिर", "पुस", "माघ", "फागुन", "चैत")
        val latin =
            listOf(
                "Baishakh",
                "Jestha",
                "Ashad",
                "Shrawan",
                "Bhadra",
                "Ashwin",
                "Kartik",
                "Mangsir",
                "Poush",
                "Magh",
                "Falgun",
                "Chaitra",
            )
        LanguageTable.languages.filter { it.code != "ne" }.forEach { language ->
            BikramSambatNames.months(language.code) shouldBe latin
        }
    }

    @Test
    fun `only Nepali has an official era and pattern`() {
        BikramSambatNames.era("ne") shouldBe "वि.सं."
        BikramSambatNames.pattern("ne") shouldBe "d MMMM, y"
        BikramSambatNames.era("en") shouldBe null
        BikramSambatNames.pattern("fa") shouldBe null
        val english = FormatTable.of(requireNotNull(LanguageTable.forCode("en")))
        english.eras.containsKey(CalendarSystem.NEPALI) shouldBe false
        english.datePatterns[CalendarSystem.NEPALI] shouldBe english.datePatterns[CalendarSystem.GREGORIAN]
        english.monthNames[CalendarSystem.NEPALI]?.last() shouldBe "Chaitra"
    }
}
