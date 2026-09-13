/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** T-304 with synthetic events; titles are search fixtures, not dataset records. */
class EventSearchIndexTest {
    private fun definition(
        id: String,
        fa: String,
        en: String? = null,
        aliases: List<String> = emptyList(),
        holiday: Boolean = false,
        source: EventSource = EventSource.INTERNATIONAL,
        category: EventCategory = EventCategory.CULTURAL,
        calendar: CalendarSystem = CalendarSystem.PERSIAN,
    ) = EventDefinition(
        id = EventId(id),
        calendar = calendar,
        source = source,
        category = category,
        isHoliday = holiday,
        title = LocalizedText(if (en == null) mapOf("fa" to fa) else mapOf("fa" to fa, "en" to en)),
        rule = EventRule.Fixed(1, 1),
        aliases = aliases,
    )

    private fun EventSearchIndex.ids(query: SearchQuery): List<String> = search(query).map { it.definition.id.value }

    @Test
    fun `Nowruz is found from Persian, Arabic-letter and Latin spellings`() {
        val index =
            EventSearchIndex(
                listOf(
                    definition("test.nowruz", fa = "نوروز", en = "Nowruz"),
                    definition("test.arbor", fa = "روز درختکاری", en = "Arbor day"),
                ),
            )

        index.search(SearchQuery("نوروز")).first().let {
            it.definition.id.value shouldBe "test.nowruz"
            it.kind shouldBe MatchKind.EXACT
        }
        // "نوريز" with ARABIC LETTER YEH (U+064A): a one-letter typo once normalized.
        index.search(SearchQuery("نوريز")).single().let {
            it.definition.id.value shouldBe "test.nowruz"
            it.kind shouldBe MatchKind.FUZZY
        }
        index.search(SearchQuery("nowruz")).single().kind shouldBe MatchKind.EXACT
        index.search(SearchQuery("NOWRUZ")).single().matchedText shouldBe "Nowruz"
    }

    @Test
    fun `exact beats prefix beats substring beats fuzzy`() {
        val index =
            EventSearchIndex(
                listOf(
                    definition("test.d", fa = "festivel"),
                    definition("test.c", fa = "the festival"),
                    definition("test.b", fa = "festivals of light"),
                    definition("test.a", fa = "festival"),
                ),
            )

        val hits = index.search(SearchQuery("festival"))

        hits.map { it.definition.id.value } shouldBe listOf("test.a", "test.b", "test.c", "test.d")
        hits.map { it.kind } shouldBe listOf(MatchKind.EXACT, MatchKind.PREFIX, MatchKind.SUBSTRING, MatchKind.FUZZY)
    }

    @Test
    fun `long queries tolerate two edits, ranked below one edit`() {
        val index =
            EventSearchIndex(listOf(definition("test.x", fa = "festival"), definition("test.z", fa = "fstivals")))

        val hits = index.search(SearchQuery("fstivall"))

        hits.map { it.definition.id.value } shouldBe listOf("test.z", "test.x")
        hits.map { it.kind }.toSet() shouldBe setOf(MatchKind.FUZZY)
        (hits[0].score > hits[1].score) shouldBe true
    }

    @Test
    fun `short queries are matched literally and four-letter queries allow one edit`() {
        val index = EventSearchIndex(listOf(definition("test.f", fa = "festival"), definition("test.q", fa = "abcd")))

        index.ids(SearchQuery("fes")) shouldBe listOf("test.f")
        index.ids(SearchQuery("fxs")).shouldBeEmpty()
        index.search(SearchQuery("abce")).single().kind shouldBe MatchKind.FUZZY
        index.ids(SearchQuery("abef")).shouldBeEmpty()
    }

    @Test
    fun `ties go to holidays and then to the id`() {
        val index =
            EventSearchIndex(
                listOf(
                    definition("test.m2", fa = "moon"),
                    definition("test.z-holiday", fa = "moon", holiday = true),
                    definition("test.m1", fa = "moon"),
                ),
            )

        index.ids(SearchQuery("moon")) shouldBe listOf("test.z-holiday", "test.m1", "test.m2")
    }

    @Test
    fun `filters restrict sources, categories, holidays and calendars`() {
        val official =
            definition(
                "test.p1",
                fa = "harvest",
                holiday = true,
                source = EventSource.IRAN_OFFICIAL,
                category = EventCategory.NATIONAL,
            )
        val cultural = definition("test.p2", fa = "harvest", calendar = CalendarSystem.GREGORIAN)
        val index = EventSearchIndex(listOf(official, cultural))

        index.ids(SearchQuery("harvest")) shouldBe listOf("test.p1", "test.p2")
        index.ids(SearchQuery("harvest", sources = setOf(EventSource.IRAN_OFFICIAL))) shouldBe listOf("test.p1")
        index.ids(SearchQuery("harvest", categories = setOf(EventCategory.CULTURAL))) shouldBe listOf("test.p2")
        index.ids(SearchQuery("harvest", holidaysOnly = true)) shouldBe listOf("test.p1")
        index.ids(SearchQuery("harvest", calendar = CalendarSystem.GREGORIAN)) shouldBe listOf("test.p2")
        SearchQuery("x", calendar = CalendarSystem.PERSIAN).accepts(cultural) shouldBe false
    }

    @Test
    fun `limits apply and a full result skips fuzzy matching`() {
        val stars = (1..30).map { definition("test.star-%02d".format(it), fa = "star $it") }
        val comets = listOf(definition("test.comet", fa = "comet"), definition("test.comat", fa = "comat"))
        val index = EventSearchIndex(stars + comets)

        index.search(SearchQuery("star")) shouldHaveSize SearchQuery.DEFAULT_LIMIT
        index.search(SearchQuery("star", limit = 5)) shouldHaveSize 5
        index.ids(SearchQuery("comet")) shouldBe listOf("test.comet", "test.comat")
        index.ids(SearchQuery("comet", limit = 1)) shouldBe listOf("test.comet")
        shouldThrow<IllegalArgumentException> { SearchQuery("comet", limit = 0) }
    }

    @Test
    fun `each event appears once with its best matching text`() {
        val index =
            EventSearchIndex(
                listOf(
                    definition("test.t", fa = "sundawn", aliases = listOf("dawn")),
                    definition("test.v", fa = "dawn", aliases = listOf("dawning")),
                    definition("test.w", fa = "morning gathering", aliases = listOf("dawn")),
                ),
            )

        val hits = index.search(SearchQuery("dawn"))

        hits.map { it.definition.id.value } shouldBe listOf("test.t", "test.v", "test.w")
        hits.map { it.kind }.toSet() shouldBe setOf(MatchKind.EXACT)
        hits.map { it.matchedText } shouldBe listOf("dawn", "dawn", "dawn")
    }

    @Test
    fun `blank queries and texts without letters find nothing`() {
        val index = EventSearchIndex(listOf(definition("test.tatweel", fa = "ـ"), definition("test.ok", fa = "ok")))

        index.search(SearchQuery("   ")).shouldBeEmpty()
        index.search(SearchQuery("ـ")).shouldBeEmpty()
        index.ids(SearchQuery("ok")) shouldBe listOf("test.ok")
    }
}
