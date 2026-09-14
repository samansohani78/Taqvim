/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-804 U/P ranking: "نور" finds Nowruz first; groups, synonyms, duplicates, limits, input-order independence. */
class SearchRankerTest {
    private val dates = SearchDates(PERSIAN_FA)

    private fun rank(
        query: String,
        events: List<SearchEvent> = sampleEvents(PERSIAN_FA),
        entries: List<SearchEntry> = sampleEntries(PERSIAN_FA),
        limit: Int = SearchRanker.DEFAULT_LIMIT,
    ): List<SearchSection> = SearchRanker.rank(query, events, entries, dates::label, limit)

    @Test
    fun `typing a word of Nowruz finds it first with its day`() {
        val sections = rank("نور")

        sections.map { it.group } shouldBe listOf(SearchGroup.EVENTS)
        val first = sections.single().results.first()
        first.title shouldBe "عید نوروز"
        first.highlight shouldBe 4..6
        first.dayLabel shouldBe "شنبه ۱ فروردین ۱۴۰۵"
        first.isHoliday shouldBe true
        first.eventKind shouldBe SearchEventKind.OFFICIAL
        first.target shouldBe SearchTarget.Event(SearchEventKind.OFFICIAL, "ir.nowruz", NOWRUZ_1405)
        sections.single().results.map { it.title } shouldBe listOf("عید نوروز", "تولد نورا")
        rank("nowruz")
            .single()
            .results
            .single()
            .matchedAlias shouldBe "Nowruz"
    }

    @Test
    fun `sections follow the group order and synonyms find settings and tools`() {
        rank("ت").map { it.group } shouldBe listOf(SearchGroup.EVENTS, SearchGroup.TOOLS, SearchGroup.SETTINGS)

        val athan = rank("نماز").single()
        athan.group shouldBe SearchGroup.SETTINGS
        athan.results.single().title shouldBe "اذان"
        athan.results.single().matchedAlias shouldBe "نماز"
        athan.results
            .single()
            .highlight
            .shouldBeNull()
        athan.results.single().target shouldBe SearchTarget.Settings(SettingsEntry.ATHAN)
        rank("طلوع")
            .single()
            .results
            .single()
            .key shouldBe "tool:PRAYER_TIMES"
        rank(" ").shouldBeEmpty()
    }

    @Test
    fun `duplicates keep their best match, holidays break ties, and sections are limited`() {
        val duplicate = sampleEvents(PERSIAN_FA).first().copy(title = "نوروز", aliases = emptyList())
        val merged = rank("نوروز", events = listOf(duplicate, sampleEvents(PERSIAN_FA).first())).single().results
        merged.map { it.key } shouldBe listOf("event:OFFICIAL:ir.nowruz")
        merged.single().title shouldBe "نوروز"

        val plain = SearchEvent("a", SearchEventKind.PERSONAL, "سفر")
        val holiday = SearchEvent("b", SearchEventKind.OFFICIAL, "سفر", isHoliday = true)
        rank("سفر", events = listOf(plain, holiday)).single().results.map { it.key } shouldBe
            listOf("event:OFFICIAL:b", "event:PERSONAL:a")

        val many = (1..30).map { SearchEvent("e$it", SearchEventKind.PERSONAL, "رویداد $it") }
        val limited = rank("رویداد", events = many, limit = 5).single().results
        limited shouldHaveSize 5
        limited.take(3).map { it.title } shouldBe listOf("رویداد 1", "رویداد 2", "رویداد 3")
        shouldThrow<IllegalArgumentException> { rank("رویداد", limit = 0) }
    }

    @Test
    fun `entries leading elsewhere are listed with the tools`() {
        val today = SearchEntry(SearchTarget.Day(TODAY), "امروز", emptyList())
        val section = rank("امروز", events = emptyList(), entries = listOf(today)).single()
        section.group shouldBe SearchGroup.TOOLS
        section.results.single().key shouldStartWith "other:"
    }

    @Test
    fun `results do not depend on the input order and keys are unique`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.list(Arb.element(POOL), 0..40),
                Arb.long(),
                Arb.int(1..6),
                Arb.element(QUERIES),
            ) { events, seed, limit, query ->
                val sections = rank(query, events = events, limit = limit)
                rank(query, events = events.shuffled(Random(seed)), limit = limit) shouldBe sections
                val keys = sections.flatMap { section -> section.results.map { it.key } }
                keys.distinct() shouldBe keys
                sections.forEach { it.results.size shouldBeLessThanOrEqual limit }
            }
        }

    private companion object {
        val KINDS = listOf(SearchEventKind.OFFICIAL, SearchEventKind.PERSONAL)
        val TITLES = listOf("نوروز", "عید نوروز", "روز نور", "نورا", "كتاب نور")
        val POOL =
            (0..24).map {
                SearchEvent(
                    id = "e${it % 5}",
                    kind = KINDS[(it / 5) % 2],
                    title = TITLES[it % TITLES.size] + " $it",
                    nextDay = if (it % 3 == 0) null else TODAY + it,
                    isHoliday = it % 4 == 0,
                )
            }
        val QUERIES = listOf("نور", "نوروز", "روز", "عيد", "كتاب", "۱")
    }
}
