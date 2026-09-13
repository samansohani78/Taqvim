/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.generated

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSorted
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.OccurrenceCalculator
import org.junit.jupiter.api.Test

/** D-08: the generated dataset loads as typed definitions the rule engine accepts. */
class OfficialEventsTest {
    @Test
    fun `generated events are sorted, unique, cited and resolvable`() {
        val events = OfficialEvents.ALL
        val ids = events.map { it.id.value }

        events.shouldNotBeEmpty()
        ids.shouldBeSorted()
        ids.toSet().size shouldBe ids.size
        events.all { it.citations.isNotEmpty() && it.title.texts.containsKey("fa") }.shouldBeTrue()
        val calculator = OccurrenceCalculator(events)
        events.flatMap { calculator.occurrences(it, YEAR_BY_CALENDAR.getValue(it.calendar.name)) }.shouldNotBeEmpty()
    }

    private companion object {
        val YEAR_BY_CALENDAR = mapOf("PERSIAN" to 1405, "ISLAMIC" to 1447, "GREGORIAN" to 2026, "NEPALI" to 2083)
    }
}
