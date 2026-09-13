/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.ISLAMIC
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn
import org.junit.jupiter.api.Test

/** U tests of T-501: scanner windows, overlap resolution by score, kinds and the confidence floor. */
class TextDateDetectorTest {
    private val reference: Jdn = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, 2026, 9, 13))
    private val english =
        ParseContext.forLanguage(requireNotNull(LanguageTable.forCode("en")), reference, GREGORIAN, SyntheticEvents)
    private val persian = ParseContext.forLanguage(requireNotNull(LanguageTable.forCode("fa")), reference)

    private fun gregorian(
        month: Int,
        day: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, 2026, month, day))

    private val filler = "lorem ipsum ".repeat(40)

    @Test
    fun `a range outranks the dates inside it and spans are positions in the whole text`() {
        val phrase = "from September 13, 2026 to September 20, 2026"
        val text = "$filler$phrase."

        val found = TextDateDetector.detect(text, english)

        found shouldHaveSize 1
        found[0].span shouldBe (filler.length until filler.length + phrase.length)
        found[0].best.kind shouldBe ParseKind.RANGE
        found[0].best.jdn shouldBe reference
        found[0].best.end?.jdn shouldBe gregorian(9, 20)
    }

    @Test
    fun `other readings of the same span are kept as alternatives`() {
        val found = TextDateDetector.detect("تاریخ نامه ۱۴۰۵/۰۶/۲۲ است", persian)

        found shouldHaveSize 1
        found[0].best.date shouldBe CalendarDate(PERSIAN, 1405, 6, 22)
        found[0].alternatives.map { it.date.system } shouldBe listOf(ISLAMIC)
    }

    @Test
    fun `dates far apart are found in separate windows and returned in text order`() {
        val text = "Paid 2026-09-20. $filler Due 2026-09-13; ${filler}then 20 September 2026 again"

        val found = TextDateDetector.detect(text, english)

        found.map { it.best.jdn } shouldBe listOf(gregorian(9, 20), reference, gregorian(9, 20))
        found.map { text.substring(it.span.first, it.span.last + 1) } shouldBe
            listOf("2026-09-20", "2026-09-13", "20 September 2026")
    }

    @Test
    fun `relative and anchored phrases are reported only when requested`() {
        val text = "See you tomorrow, and the party is 2 days before Sample Festival."
        TextDateDetector.detect(text, english).shouldBeEmpty()

        val all = DetectionOptions(kinds = ParseKind.entries.toSet())
        val found = TextDateDetector.detect(text, english, all)

        found.map { it.best.kind } shouldBe listOf(ParseKind.RELATIVE, ParseKind.ANCHORED)
        found[0].best.jdn shouldBe reference + 1
        found[1].best.jdn shouldBe gregorian(5, 18)
        TextDateDetector.detect("امروز", persian, DetectionOptions(setOf(ParseKind.RELATIVE)))[0].best.jdn shouldBe
            reference
    }

    @Test
    fun `the confidence floor drops weak readings`() {
        val text = "code 22 6 1405 here"
        TextDateDetector.detect(text, persian).shouldBeEmpty()

        val found = TextDateDetector.detect(text, persian, DetectionOptions(minConfidence = 0.5))

        found.single().best.date shouldBe CalendarDate(PERSIAN, 1405, 6, 22)
        found.single().span shouldBe (5..13)
    }

    @Test
    fun `seed windows merge when they meet and text without seeds is not parsed`() {
        val near = Tokenizer.tokenize("1 ${"word ".repeat(TextDateDetector.WINDOW_TOKENS * 2)}2")
        TextDateDetector.segments(near, DetectionOptions.DEFAULT_KINDS) shouldHaveSize 1
        val far = Tokenizer.tokenize("1 ${"word ".repeat(TextDateDetector.WINDOW_TOKENS * 2 + 1)}2")
        TextDateDetector.segments(far, DetectionOptions.DEFAULT_KINDS) shouldHaveSize 2

        val words = Tokenizer.tokenize("see you tomorrow")
        TextDateDetector.segments(words, DetectionOptions.DEFAULT_KINDS).shouldBeEmpty()
        TextDateDetector.segments(words, setOf(ParseKind.ANCHORED)) shouldBe listOf(0..15)
        TextDateDetector.detect("", persian).shouldBeEmpty()
        Lexicon.isSeedWord(Tokenizer.tokenize("12")[0]) shouldBe false
    }

    @Test
    fun `an ambiguous numeric date keeps the context order first`() {
        val found = TextDateDetector.detect("on 05/06/2026 at noon", english)

        found.single().best.jdn shouldBe gregorian(5, 6)
        found.single().alternatives.map { it.jdn } shouldBe listOf(gregorian(6, 5))
        PersianCalendarSystem.fromJdn(found.single().best.jdn).year shouldBe 1405
    }
}
