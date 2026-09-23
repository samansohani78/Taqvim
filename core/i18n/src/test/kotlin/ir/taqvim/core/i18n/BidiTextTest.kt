/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import java.text.Bidi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1701: isolates keep embedded runs in their own direction inside a right-to-left paragraph (java.text.Bidi). */
class BidiTextTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    /** Embedding levels of [text] laid out in a right-to-left paragraph. */
    private fun rtlLevels(text: String): List<Int> {
        val bidi = Bidi(text, Bidi.DIRECTION_RIGHT_TO_LEFT)
        return text.indices.map { bidi.getLevelAt(it) }
    }

    @Test
    fun `an unisolated expression is reordered around its operator in a Persian paragraph`() {
        val text = "مجموع ۲۵ + ۱"
        val levels = rtlLevels(text)
        (levels[text.indexOf('+')] % 2) shouldBe 1
    }

    @Test
    fun `a left-to-right isolate keeps numbers, operators, units and coordinates left to right`() {
        listOf("۲۵ + ۱", "1d 2h + 30m", "35.6892, 51.3890", "1.2.3-beta.4 (debug)", "https://example.com/a?b=1")
            .forEach { run ->
                val text = "مقدار " + BidiText.ltr(run) + " است"
                val start = text.indexOf(BidiText.LRI) + 1
                val end = text.indexOf(BidiText.PDI)
                rtlLevels(text).subList(start, end).all { it % 2 == 0 } shouldBe true
            }
    }

    @Test
    fun `first strong and right-to-left isolates follow their content`() {
        val latin = "نسخه " + BidiText.isolate("debug") + " ۴۲"
        val start = latin.indexOf(BidiText.FSI) + 1
        rtlLevels(latin).subList(start, start + "debug".length).all { it % 2 == 0 } shouldBe true

        val persian = "Version " + BidiText.rtl("۴۲ تست") + " done"
        val levels = Bidi(persian, Bidi.DIRECTION_LEFT_TO_RIGHT).let { bidi -> persian.indices.map(bidi::getLevelAt) }
        val inner = persian.indexOf(BidiText.RLI) + 1
        (levels[persian.indexOf('ت', inner)] % 2) shouldBe 1
    }

    @Test
    fun `isolates are balanced and strip restores the text`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.string()) { raw ->
                val text = BidiText.strip(raw)
                listOf(BidiText.ltr(text), BidiText.rtl(text), BidiText.isolate(text)).forEach { wrapped ->
                    BidiText.strip(wrapped) shouldBe text
                    wrapped.count { it == BidiText.PDI } shouldBe 1
                    wrapped.last() shouldBe BidiText.PDI
                }
            }
        }

    @Test
    fun `isolates are balanced for the empty string and text already carrying isolate marks`() {
        listOf("", BidiText.ltr("x"), BidiText.rtl("x"), BidiText.isolate("x"), "${BidiText.PDI}${BidiText.LRI}")
            .forEach { raw ->
                val text = BidiText.strip(raw)
                listOf(BidiText.ltr(text), BidiText.rtl(text), BidiText.isolate(text)).forEach { wrapped ->
                    BidiText.strip(wrapped) shouldBe text
                    wrapped.count { it == BidiText.PDI } shouldBe 1
                    wrapped.last() shouldBe BidiText.PDI
                }
            }
    }
}
