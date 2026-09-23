/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ContentLinesTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    @Test
    fun `unfolding joins CRLF, LF and tab continuations and keeps line numbers`() {
        ContentLines.unfold("A:1\r\n B\r\nC:2\n\tD\n\nE:3") shouldBe listOf(1 to "A:1B", 3 to "C:2D", 6 to "E:3")
        ContentLines.unfold(" orphan continuation\nA:1") shouldBe listOf(1 to " orphan continuation", 2 to "A:1")
        ContentLines.unfold("").shouldBeEmpty()
    }

    @Test
    fun `content lines keep quoted separators and multiple values`() {
        val line =
            ContentLines.parse(
                7,
                "attendee;CN=\"Doe, Jane: Lead\";delegated-to=\"a:b\",c;ROLE=X:mailto:j@example.org",
            )

        line shouldBe
            ContentLine(
                7,
                "ATTENDEE",
                mapOf("CN" to listOf("Doe, Jane: Lead"), "DELEGATED-TO" to listOf("a:b", "c"), "ROLE" to listOf("X")),
                "mailto:j@example.org",
            )
        line?.parameter("ROLE") shouldBe "X"
        line?.parameter("MISSING").shouldBeNull()
        ContentLines.parse(1, "SUMMARY:") shouldBe ContentLine(1, "SUMMARY", emptyMap(), "")
    }

    @Test
    fun `malformed content lines are rejected`() {
        listOf(
            "NOCOLON",
            ":value",
            "NA ME:value",
            "NAME;PARAM:value",
            "NAME;=x:value",
            "NAME;P=\"unterminated:value",
            "NAME;P=\"quoted\"trailing:value",
            "NAME;P=a;",
            "NAME;P=a",
        ).forEach { ContentLines.parse(1, it).shouldBeNull() }
    }

    @Test
    fun `text escaping follows section 3-3-11`() {
        ContentLines.escapeText("a\\b;c,d\ne\r\nf") shouldBe "a\\\\b\\;c\\,d\\ne\\nf"
        ContentLines.unescapeText("a\\\\b\\;c\\,d\\ne\\Nf\\x\\") shouldBe "a\\b;c,d\ne\nfx\\"
    }

    @Test
    fun `folding respects 75 octets without splitting characters`() {
        val line = "SUMMARY:" + "a".repeat(70) + "ش".repeat(40) + "😀".repeat(10)
        val physical = ContentLines.fold(line)

        physical.forEach { it.toByteArray(Charsets.UTF_8).size shouldBeLessThanOrEqual ContentLines.MAX_OCTETS }
        ContentLines.unfold(physical.joinToString("\r\n")).single().second shouldBe line
        ContentLines.fold("SHORT:x") shouldBe listOf("SHORT:x")
        listOf(0x41, 0x634, 0x4E2D, 0x1F600).map(ContentLines::utf8Width) shouldBe listOf(1, 2, 3, 4)
    }

    @Test
    fun `escaping and folding round-trip any text`(): Unit =
        runBlocking {
            val codepoints =
                (
                    listOf('a', 'Z', '0', ' ', '\t', ';', ',', ':', '"', '\\', '\n').map { Codepoint(it.code) } +
                        listOf(0x0627, 0x0634, 0x06CC, 0x4E2D, 0x1F600).map(::Codepoint)
                )
            checkAll(propertyConfig, Arb.string(0..200, Arb.element(codepoints))) { text ->
                val physical = ContentLines.fold("DESCRIPTION:" + ContentLines.escapeText(text))
                val unfolded = ContentLines.unfold(physical.joinToString("\r\n")).single().second
                ContentLines.unescapeText(unfolded.removePrefix("DESCRIPTION:")) shouldBe text
            }
        }

    @Test
    fun `escaping and folding round-trip the empty value and the longest value`() {
        // A fixed seed permanently commits checkAll to one set of lengths drawn from 0..200; pin both ends so an
        // empty description and a 200-character one (which forces multiple folded physical lines) are never left
        // untested purely by chance.
        listOf(
            "",
            "a".repeat(70) + "ش".repeat(70) + "😀".repeat(15),
        ).forEach { text ->
            val physical = ContentLines.fold("DESCRIPTION:" + ContentLines.escapeText(text))
            val unfolded = ContentLines.unfold(physical.joinToString("\r\n")).single().second
            ContentLines.unescapeText(unfolded.removePrefix("DESCRIPTION:")) shouldBe text
        }
    }
}
