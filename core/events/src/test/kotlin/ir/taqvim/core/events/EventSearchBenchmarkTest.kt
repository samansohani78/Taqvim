/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import org.junit.jupiter.api.Test

/** T-304 budget: 10 000 mixed queries (exact, prefix, substring, one typo) over 1 000 synthetic events < 200 ms. */
class EventSearchBenchmarkTest {
    private val random = Random(SEED)

    private fun word(): String =
        (1..random.nextInt(MIN_WORD, MAX_WORD)).map { 'a' + random.nextInt(LETTERS) }.joinToString("")

    private fun definition(index: Int) =
        EventDefinition(
            id = EventId("test.bench.$index"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.CULTURAL,
            isHoliday = index % HOLIDAY_EVERY == 0,
            title = LocalizedText(mapOf("fa" to "${word()} ${word()}", "en" to word())),
            rule = EventRule.Fixed(1, 1),
            aliases = listOf(word()),
        )

    @Test
    fun `10 000 mixed queries over 1 000 events take less than 200 ms`() {
        val definitions = (1..EVENTS).map(::definition)
        val index = EventSearchIndex(definitions)
        val titles = definitions.map { it.title.texts.getValue("en") }
        val queries =
            List(QUERIES) { position ->
                val title = titles.random(random)
                when (position % 4) {
                    0 -> title
                    1 -> title.take(3)
                    2 -> title.substring(1, 5)
                    else -> title.replaceRange(2, 3, "q")
                }
            }
        repeat(WARM_UP_RUNS) { queries.forEach { index.search(SearchQuery(it)) } }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { queries.forEach { index.search(SearchQuery(it)) } } }

        best shouldBeLessThan BUDGET_MILLIS
        index
            .search(SearchQuery(titles.first()))
            .first()
            .definition.title.texts
            .getValue("en") shouldBe titles.first()
    }

    private companion object {
        const val SEED = 304
        const val EVENTS = 1_000
        const val QUERIES = 10_000
        const val MIN_WORD = 5
        const val MAX_WORD = 10
        const val LETTERS = 26
        const val HOLIDAY_EVERY = 7
        const val WARM_UP_RUNS = 3
        const val MEASURED_RUNS = 5
        const val BUDGET_MILLIS = 200L
    }
}
