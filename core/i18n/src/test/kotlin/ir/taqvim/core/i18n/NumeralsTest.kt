/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bigDecimal
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class NumeralsTest {
    private val systems = Arb.enum<NumeralSystem>()

    @Test
    fun `parses Persian decimal with the Arabic decimal separator`() {
        Numerals.parseDecimal("۱۲٫۵")?.toDouble() shouldBe 12.5
    }

    @TestFactory
    fun `digits and separators per system`() =
        listOf(
            Triple(NumeralSystem.LATIN, 1_234_567L, "1,234,567"),
            Triple(NumeralSystem.PERSIAN, 1_234_567L, "۱٬۲۳۴٬۵۶۷"),
            Triple(NumeralSystem.EASTERN_ARABIC, 1_234_567L, "١٬٢٣٤٬٥٦٧"),
            Triple(NumeralSystem.DEVANAGARI, 1_234_567L, "१२,३४,५६७"),
            Triple(NumeralSystem.TAMIL, 1_234_567L, "௧௨,௩௪,௫௬௭"),
            Triple(NumeralSystem.LATIN, 999L, "999"),
            Triple(NumeralSystem.DEVANAGARI, 1_000L, "१,०००"),
            Triple(NumeralSystem.DEVANAGARI, 100_000L, "१,००,०००"),
            Triple(NumeralSystem.PERSIAN, -1_405L, "-۱٬۴۰۵"),
            Triple(NumeralSystem.LATIN, Long.MIN_VALUE, "-9,223,372,036,854,775,808"),
        ).map { (system, value, expected) ->
            DynamicTest.dynamicTest("$system $value") {
                Numerals.format(value, system, grouped = true) shouldBe expected
            }
        }

    @Test
    fun `ungrouped and decimal formatting`() {
        Numerals.format(1405L, NumeralSystem.PERSIAN) shouldBe "۱۴۰۵"
        Numerals.format(BigDecimal("12.50"), NumeralSystem.PERSIAN) shouldBe "۱۲٫۵۰"
        Numerals.format(BigDecimal("-1234.5"), NumeralSystem.LATIN, grouped = true) shouldBe "-1,234.5"
        Numerals.format(BigDecimal("1E+3"), NumeralSystem.EASTERN_ARABIC) shouldBe "١٠٠٠"
        Numerals.localizeDigits("1405-01-01", NumeralSystem.PERSIAN) shouldBe "۱۴۰۵-۰۱-۰۱"
        NumeralSystem.PERSIAN.decimalSeparator shouldBe '٫'
        shouldThrow<IllegalArgumentException> { NumeralSystem.LATIN.digit(10) }
    }

    @Test
    fun `lenient parsing accepts any supported digits and rejects everything else`() {
        Numerals.parseLong("۱٬۴۰۵") shouldBe 1405L
        Numerals.parseLong("‏-۲۵") shouldBe -25L
        Numerals.parseLong("−७") shouldBe -7L
        Numerals.parseLong("+௧௨") shouldBe 12L
        Numerals.parseLong("۱2٣") shouldBe 123L
        Numerals.parseLong(" 1 234 ") shouldBe null
        Numerals.parseLong("1 234") shouldBe 1234L
        Numerals.parseLong("12.5") shouldBe null
        Numerals.parseLong("") shouldBe null
        Numerals.parseLong("-") shouldBe null
        Numerals.parseLong("12a") shouldBe null
        Numerals.parseLong("99999999999999999999") shouldBe null
        Numerals.parseDecimal("١٢.٥") shouldBe BigDecimal("12.5")
        Numerals.parseDecimal("1.2.3") shouldBe null
        Numerals.digitValue('۷') shouldBe 7
        Numerals.digitValue('x') shouldBe null
    }

    @TestFactory
    fun `traditional Tamil numerals`() =
        listOf(
            1L to "௧",
            9L to "௯",
            10L to "௰",
            11L to "௰௧",
            12L to "௰௨",
            20L to "௨௰",
            45L to "௪௰௫",
            100L to "௱",
            101L to "௱௧",
            110L to "௱௰",
            200L to "௨௱",
            999L to "௯௱௯௰௯",
            1_000L to "௲",
            1_001L to "௲௧",
            2_026L to "௨௲௨௰௬",
            10_000L to "௰௲",
            100_000L to "௱௲",
            1_000_000L to "௲௲",
        ).map { (value, expected) ->
            DynamicTest.dynamicTest("$value") {
                TamilTraditionalNumerals.format(value) shouldBe expected
                TamilTraditionalNumerals.parse(expected) shouldBe value
            }
        }

    @Test
    fun `traditional Tamil rejects zero and malformed input`() {
        shouldThrow<IllegalArgumentException> { TamilTraditionalNumerals.format(0) }
        TamilTraditionalNumerals.parse("") shouldBe null
        TamilTraditionalNumerals.parse("௰௱") shouldBe null
        TamilTraditionalNumerals.parse("௦") shouldBe null
        TamilTraditionalNumerals.parse("abc") shouldBe null
        TamilTraditionalNumerals.parse("x௲") shouldBe null
        TamilTraditionalNumerals.parse("௲x") shouldBe null
        TamilTraditionalNumerals.parse("௲".repeat(8)) shouldBe null
    }

    @Test
    fun `integers round-trip through every system`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(), systems, Arb.boolean()) { value, system, grouped ->
                Numerals.parseLong(Numerals.format(value, system, grouped)) shouldBe value
            }
        }

    @Test
    fun `decimals round-trip through every system`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.bigDecimal(), systems, Arb.boolean()) { value, system, grouped ->
                Numerals.parseDecimal(Numerals.format(value, system, grouped))?.compareTo(value) shouldBe 0
            }
        }

    @Test
    fun `traditional Tamil round-trips`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(1L..1_000_000_000_000L)) { value ->
                TamilTraditionalNumerals.parse(TamilTraditionalNumerals.format(value)) shouldBe value
            }
        }
}
