/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-804: "go to date" readings, result day labels, recent searches and today. */
class SearchDatesTest {
    @Test
    fun `a written date is offered as a day in the language's calendar`() {
        val fa = SearchDates(PERSIAN_FA)
        fa.label(NOWRUZ_1405) shouldBe "شنبه ۱ فروردین ۱۴۰۵"
        val suggestion = fa.suggest("۱ فروردین ۱۴۰۵", TODAY).shouldNotBeNull()
        suggestion shouldBe DateSuggestion(NOWRUZ_1405, "شنبه ۱ فروردین ۱۴۰۵")

        val en = SearchDates(GREGORIAN_EN)
        val english = en.suggest("March 21, 2026", TODAY)
        english shouldBe DateSuggestion(NOWRUZ_1405, "Saturday, March 21, 2026")
    }

    @Test
    fun `words, ranges, blanks and dates inside longer text are not offered`() {
        val fa = SearchDates(PERSIAN_FA)
        fa.suggest("نوروز", TODAY).shouldBeNull()
        fa.suggest("  ", TODAY).shouldBeNull()
        fa.suggest("از ۱ تا ۵ فروردین ۱۴۰۵", TODAY).shouldBeNull()
        fa.suggest("جلسه بررسی بودجه سالانه با تیم فروش ۱ فروردین", TODAY).shouldBeNull()
    }

    @Test
    fun `unknown languages and calendars fall back`() {
        val fallback = SearchDates(SearchSettings("xx", listOf(CalendarSystem.NEPALI)))
        fallback.language.code shouldBe LanguageTable.languages.first().code
        fallback.label(NOWRUZ_1405).shouldNotBeNull()
    }

    @Test
    fun `recent searches are newest first, distinct by key, capped and clearable`(): Unit =
        runTest {
            val store = SessionRecentQueriesStore(capacity = 3)
            store.add("نوروز")
            store.add(" قبله ")
            store.add("   ")
            store.add("علي")
            store.add("علی")
            store.queries().first() shouldContainExactly listOf("علی", "قبله", "نوروز")
            store.add("تقویم")
            store.queries().first() shouldContainExactly listOf("تقویم", "علی", "قبله")
            store.clear()
            store.queries().first() shouldBe emptyList()
            shouldThrow<IllegalArgumentException> { SessionRecentQueriesStore(capacity = 0) }
        }

    @Test
    fun `today is read from the provider`(): Unit =
        runBlocking {
            TickingSearchTodaySource({ TODAY }).today().first() shouldBe TODAY
            shouldThrow<IllegalArgumentException> {
                TickingSearchTodaySource({ TODAY }, kotlin.time.Duration.ZERO)
            }
            ResourceSearchCatalog.splitKeywords(" a | |b|") shouldBe listOf("a", "b")
        }
}
