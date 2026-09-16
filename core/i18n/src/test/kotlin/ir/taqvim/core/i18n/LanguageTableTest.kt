/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class LanguageTableTest {
    private val languages = LanguageTable.languages

    @Test
    fun `the launch languages are the 24 approved in ADR-0007`() {
        languages.map { it.code } shouldBe APPROVED
    }

    @TestFactory
    fun `every language has every field`() =
        languages.map { spec ->
            DynamicTest.dynamicTest(spec.code) {
                listOf(spec.localeTag, spec.nativeName, spec.script, spec.dayPeriods.am, spec.dayPeriods.pm)
                    .forEach { it.shouldNotBeBlank() }
                spec.calendars.shouldNotBeEmpty()
                spec.weekend.shouldNotBeEmpty()
                spec.monthNames.gregorian.size shouldBe 12
                spec.localeTag.substringBefore('-') shouldBe CLDR_LANGUAGE.getOrDefault(spec.code, spec.code)
            }
        }

    @Test
    fun `data gaps are exactly the ones recorded in DATA_TODO`() {
        fun missing(select: (LanguageSpec) -> Any?) = languages.filter { select(it) == null }.map { it.code }.toSet()

        missing { it.andPattern } shouldBe setOf("ckb")
        missing { it.monthNames.persian } shouldBe setOf("ckb", "kmr", "ne", "id", "ms", "zh")
        missing { it.monthNames.islamic } shouldBe setOf("ckb", "ne", "zh")
        // Bikram Sambat names come from official Nepali sources for every language (bikram-sambat.properties).
        missing { it.monthNames.nepali } shouldBe emptySet()
    }

    @Test
    fun `numerals and direction follow the ADR-0007 rules`() {
        languages.forEach { spec ->
            withClue(spec.code) {
                spec.numerals shouldBe NATIVE_NUMERALS.getOrDefault(spec.code, NumeralSystem.LATIN)
                spec.direction shouldBe if (spec.script == "Arab") TextDirection.RTL else TextDirection.LTR
            }
        }
    }

    @Test
    fun `lookup, month variants and and-joining`() {
        val english = LanguageTable.forCode("en")
        english?.joinWithAnd("{1}", "{0}") shouldBe "{1} and {0}"
        english?.monthNames?.forSystem(CalendarSystem.GREGORIAN)?.first() shouldBe "January"
        english?.monthNames?.forSystem(CalendarSystem.PERSIAN)?.first() shouldBe "Farvardin"
        english?.monthNames?.forSystem(CalendarSystem.ISLAMIC)?.first() shouldBe "Muharram"
        english?.monthNames?.forSystem(CalendarSystem.NEPALI)?.first() shouldBe "Baishakh"
        LanguageTable
            .forCode("ne")
            ?.monthNames
            ?.forSystem(CalendarSystem.NEPALI)
            ?.first() shouldBe "वैशाख"
        LanguageTable.forCode("ckb")?.joinWithAnd("a", "b") shouldBe null
        LanguageTable.forCode("xx") shouldBe null
        LanguageTable.forCode("prs")?.nativeName shouldNotBe LanguageTable.forCode("fa")?.nativeName
    }

    @Test
    fun `table snapshot`() {
        val snapshot = File("src/test/resources/snapshots/language-table.json")

        SnapshotVerifier().verify(snapshot, LanguageTableJson.render(languages), "LanguageTableTest").getOrThrow()
    }

    private companion object {
        val APPROVED =
            listOf(
                "fa",
                "prs",
                "ps",
                "ar",
                "ckb",
                "kmr",
                "az",
                "tr",
                "ur",
                "ne",
                "hi",
                "ta",
                "bn",
                "tg",
                "uz",
                "en",
                "ru",
                "de",
                "fr",
                "es",
                "id",
                "ms",
                "zh",
                "ja",
            )

        /** Codes whose CLDR locale uses a different language subtag. */
        val CLDR_LANGUAGE = mapOf("prs" to "fa", "kmr" to "ku")

        val NATIVE_NUMERALS =
            mapOf(
                "fa" to NumeralSystem.PERSIAN,
                "prs" to NumeralSystem.PERSIAN,
                "ps" to NumeralSystem.PERSIAN,
                "ar" to NumeralSystem.EASTERN_ARABIC,
                "ckb" to NumeralSystem.EASTERN_ARABIC,
                "ne" to NumeralSystem.DEVANAGARI,
            )
    }
}
