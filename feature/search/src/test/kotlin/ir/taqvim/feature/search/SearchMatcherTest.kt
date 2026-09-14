/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-804 U/P: Persian-insensitive keys, match kinds and their order, typo tolerance and highlighting. */
class SearchMatcherTest {
    private fun kindOf(
        query: String,
        vararg texts: String,
    ): MatchKind? = SearchMatcher.match(query, texts.toList())?.kind

    @Test
    fun `letter variants, ZWNJ, spaces, case and digit scripts do not matter`() {
        kindOf("كتاب", "کتاب") shouldBe MatchKind.EXACT
        kindOf("علي", "علی") shouldBe MatchKind.EXACT
        kindOf("می‌روم", "میروم") shouldBe MatchKind.EXACT
        kindOf("نو روز", "نوروز") shouldBe MatchKind.EXACT
        kindOf("۱۴۰۵", "Nowruz 1405") shouldBe MatchKind.WORD_PREFIX
        kindOf("1405", "نوروز ١٤٠٥") shouldBe MatchKind.WORD_PREFIX
        kindOf("NOWRUZ", "Nowruz") shouldBe MatchKind.EXACT
        SearchMatcher.key("آغاز‌نوروز ۱۴۰۵") shouldBe "اغازنوروز1405"
    }

    @Test
    fun `kinds rank exact over prefix over word prefix over substring over typos`() {
        val exact = SearchMatcher.match("نوروز", listOf("نوروز")).shouldNotBeNull()
        val prefix = SearchMatcher.match("نور", listOf("نوروز")).shouldNotBeNull()
        val longerPrefix = SearchMatcher.match("نور", listOf("نورانی")).shouldNotBeNull()
        val word = SearchMatcher.match("نور", listOf("عید نوروز")).shouldNotBeNull()
        val laterWord = SearchMatcher.match("نور", listOf("جشن عید نوروز")).shouldNotBeNull()
        val substring = SearchMatcher.match("ورو", listOf("عید نوروز")).shouldNotBeNull()
        val typo = SearchMatcher.match("نوروض", listOf("عید نوروز")).shouldNotBeNull()

        listOf(prefix.kind, word.kind, substring.kind, typo.kind) shouldBe
            listOf(MatchKind.PREFIX, MatchKind.WORD_PREFIX, MatchKind.SUBSTRING, MatchKind.FUZZY)
        exact.score shouldBeGreaterThan prefix.score
        prefix.score shouldBeGreaterThan longerPrefix.score
        longerPrefix.score shouldBeGreaterThan word.score
        word.score shouldBeGreaterThan laterWord.score
        laterWord.score shouldBeGreaterThan substring.score
        substring.score shouldBeGreaterThan typo.score
        val best = SearchMatcher.match("نور", listOf("عید نوروز", "نور"))
        best shouldBe TextMatch("نور", MatchKind.EXACT, exact.score)
    }

    @Test
    fun `typos are tolerated only for long enough queries`() {
        kindOf("نوری", "عید نورا") shouldBe MatchKind.FUZZY
        kindOf("نوری", "نوروزها").shouldBeNull()
        kindOf("نری", "نوری").shouldBeNull()
        kindOf("تقویمم", "تقوم") shouldBe MatchKind.FUZZY
        kindOf("", "نوروز").shouldBeNull()
        kindOf("‌ ", "نوروز").shouldBeNull()
        kindOf("نور", "").shouldBeNull()
        kindOf("نوروز", "‌").shouldBeNull()
    }

    @Test
    fun `highlights cover the query in the original text`() {
        SearchMatcher.highlight("عید نوروز", "نور") shouldBe 4..6
        SearchMatcher.highlight("تقویم‌ها", "تقویمها") shouldBe 0..7
        SearchMatcher.highlight("Nowruz 1405", "۱۴۰۵") shouldBe 7..10
        SearchMatcher.highlight("عید نوروز", "نوروض").shouldBeNull()
        SearchMatcher.highlight("عید نوروز", " ").shouldBeNull()
    }

    @Test
    fun `any piece of a text is found, highlighted and keyed like the query`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, TEXTS, Arb.int(0..40), Arb.int(0..40)) { text, a, b ->
                val from = minOf(a, b).coerceAtMost(text.length)
                val until = maxOf(a, b).coerceAtMost(text.length)
                val query = text.substring(from, until)
                val queryKey = SearchMatcher.key(query)
                SearchMatcher.key(queryKey) shouldBe queryKey
                if (queryKey.isNotEmpty()) {
                    SearchMatcher.match(query, listOf(text)).shouldNotBeNull()
                    val range = SearchMatcher.highlight(text, query).shouldNotBeNull()
                    SearchMatcher.key(text.substring(range.first, range.last + 1)) shouldBe queryKey
                }
            }
        }

    @Test
    fun `variant spellings of a text have the same key`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, TEXTS) { text ->
                val variant =
                    text
                        .replace('ی', 'ي')
                        .replace('ک', 'ك')
                        .replace("‌", "")
                        .replace(' ', '‌')
                        .replace('1', '۱')
                SearchMatcher.key(variant) shouldBe SearchMatcher.key(text)
            }
        }

    private companion object {
        val ALPHABET =
            listOf('ن', 'و', 'ر', 'ز', 'ی', 'ي', 'ک', 'ك', 'ا', 'آ', ' ', '‌', '1', '۱', 'N', 'o')
        val TEXTS = Arb.list(Arb.element(ALPHABET), 0..30).map { it.joinToString("") }
    }
}
