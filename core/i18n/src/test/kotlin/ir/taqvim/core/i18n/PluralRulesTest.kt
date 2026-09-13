/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.text.PluralRules as IcuPluralRules
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.PluralCategory.FEW
import ir.taqvim.core.i18n.PluralCategory.MANY
import ir.taqvim.core.i18n.PluralCategory.ONE
import ir.taqvim.core.i18n.PluralCategory.OTHER
import ir.taqvim.core.i18n.PluralCategory.ZERO
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PluralRulesTest {
    @Test
    fun `conditions with modulus, ranges, and and or select categories`() {
        val rules =
            PluralRules.parse(
                mapOf(
                    ONE to "v = 0 and i % 10 = 1 and i % 100 != 11",
                    FEW to "v = 0 and i % 10 = 2..4 and i % 100 != 12..14",
                    MANY to "v = 0 and i % 10 = 0 or v = 0 and i % 10 = 5..9 or v = 0 and i % 100 = 11..14",
                    OTHER to "",
                ),
            )

        listOf(1L to ONE, 21L to ONE, 11L to MANY, 2L to FEW, 22L to FEW, 12L to MANY, 0L to MANY, 111L to MANY)
            .forEach { (count, category) -> rules.select(count) shouldBe category }
        rules.select(-21) shouldBe ONE
        rules.categories shouldBe setOf(ONE, FEW, MANY, OTHER)
    }

    @Test
    fun `operands other than n and i are zero for integers`() {
        val rules =
            PluralRules.parse(
                mapOf(
                    ZERO to "n = 0,7..9",
                    ONE to "i = 1 and v = 0",
                    MANY to "e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5",
                ),
            )

        rules.select(0) shouldBe ZERO
        rules.select(8) shouldBe ZERO
        rules.select(1) shouldBe ONE
        rules.select(2_000_000) shouldBe MANY
        rules.select(2) shouldBe OTHER
    }

    @Test
    fun `malformed rules and out-of-range counts are rejected`() {
        shouldThrow<IllegalArgumentException> { PluralRules.parse(mapOf(ONE to "x = 1")) }
        shouldThrow<IllegalArgumentException> { PluralRules.parse(mapOf(ONE to "n == 1")) }
        shouldThrow<IllegalArgumentException> { PluralRules.parse(emptyMap()).select(Long.MIN_VALUE) }
    }

    @TestFactory
    fun `integer categories match ICU4J for every language`(): List<DynamicTest> =
        LanguageTable.languages.map { language ->
            DynamicTest.dynamicTest(language.code) {
                val icu = IcuPluralRules.forLocale(language.icuLocale)
                val ours = FormatTable.of(language).pluralRules
                COUNTS.filter { ours.select(it).name.lowercase() != icu.select(it.toDouble()) }.shouldBeEmpty()
            }
        }

    private companion object {
        val COUNTS = (0L..1_000L) + listOf(10_000L, 100_000L, 1_000_000L, 1_000_001L, 2_000_000L, 21_000_000L)
    }
}
