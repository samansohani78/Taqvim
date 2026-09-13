/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlin.system.measureTimeMillis
import org.junit.jupiter.api.Test

/** T-301: year cache and day lookup across calendars. */
class EventLookupTest {
    private val all = EventSource.entries.toSet()

    private fun gregorian(
        year: Int,
        month: Int,
        day: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day))

    private fun EventDefinition.from(source: EventSource) = copy(source = source)

    @Test
    fun `a day merges the calendars in holiday, source and id order`() {
        val nowruz = event("test.nowruz", CalendarSystem.PERSIAN, EventRule.Fixed(1, 1), isHoliday = true)
        val fitr = event("test.fitr", CalendarSystem.ISLAMIC, EventRule.Fixed(10, 1), isHoliday = true)
        val spring = event("test.spring", CalendarSystem.GREGORIAN, EventRule.Fixed(3, 21))
        val lookup =
            EventLookup(listOf(spring, fitr.from(EventSource.IRAN_OFFICIAL), nowruz.from(EventSource.IRAN_OFFICIAL)))

        // 1 Farvardin 1405 and the official 1 Shawwal 1447 are both 21 March 2026.
        lookup.eventsOn(gregorian(2026, 3, 21), all).map { it.definition.id.value } shouldBe
            listOf("test.fitr", "test.nowruz", "test.spring")
        lookup.eventsOn(gregorian(2026, 3, 22), all).shouldBeEmpty()
    }

    @Test
    fun `disabled sources are skipped unless the event is always displayed`() {
        val national = event("test.national", CalendarSystem.GREGORIAN, EventRule.Fixed(5, 1))
        val pinned =
            event(
                "test.pinned",
                CalendarSystem.GREGORIAN,
                EventRule.Fixed(5, 1),
            ).copy(flags = setOf(EventFlag.ALWAYS_DISPLAYED))
        val lookup = EventLookup(listOf(national, pinned))

        lookup.eventsOn(gregorian(2026, 5, 1), setOf(EventSource.IRAN_OFFICIAL)).map { it.definition.id.value } shouldBe
            listOf("test.pinned")
        lookup.occurrencesIn(CalendarSystem.GREGORIAN, 2026, all).map { it.definition.id.value } shouldBe
            listOf("test.national", "test.pinned")
    }

    @Test
    fun `occurrences pushed into the next year by an offset are found`() {
        val afterLastFriday =
            event(
                "test.after-last-friday",
                CalendarSystem.GREGORIAN,
                EventRule.LastWeekdayOfMonth(12, Weekday.FRIDAY, 7),
            )
        val lookup = EventLookup(listOf(afterLastFriday))

        // Last Friday of December 2026 is the 25th; a week later is 1 January 2027, still attributed to rule year 2026.
        val found = lookup.eventsOn(gregorian(2027, 1, 1), all).single()
        found.year shouldBe 2026
    }

    @Test
    fun `repeated lookups hit the cache and the cache stays bounded`() {
        val definitions = listOf(event("test.monthly", CalendarSystem.GREGORIAN, EventRule.LastDayOfMonth(6)))
        val lookup = EventLookup(definitions)
        val day = gregorian(2026, 6, 30)

        lookup.eventsOn(day, all)
        val afterFirst = lookup.stats
        lookup.eventsOn(day, all)
        val afterSecond = lookup.stats

        afterFirst.hits shouldBe 0L
        afterSecond.misses shouldBe afterFirst.misses
        afterSecond.hits shouldBe afterFirst.misses
        lookup.eventsOn(day, setOf(EventSource.INTERNATIONAL)).size shouldBe 1
        lookup.stats.misses shouldBe afterFirst.misses * 2

        val small = EventLookup(definitions, capacity = 4)
        (2000..2040).forEach { small.occurrencesIn(CalendarSystem.GREGORIAN, it, all) }
        small.stats.size shouldBe 4
        small.occurrencesIn(CalendarSystem.GREGORIAN, 2000, all)
        small.stats.misses shouldBe 42L
        shouldThrow<IllegalArgumentException> { EventLookup(emptyList(), capacity = 0) }
    }

    @Test
    fun `calendars that are not available are ignored`() {
        val nepali = event("test.nepali", CalendarSystem.NEPALI, EventRule.Fixed(1, 1))

        EventLookup(listOf(nepali)).eventsOn(gregorian(2026, 4, 14), all).shouldBeEmpty()
    }

    @Test
    fun `10 000 warm day lookups take less than 50 ms`() {
        val lookup = EventLookup(benchmarkDefinitions())
        val first = gregorian(2020, 1, 1).value
        val days = List(DAYS) { Jdn(first + it) }
        repeat(WARM_UP_RUNS) { days.forEach { lookup.eventsOn(it, all) } }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { days.forEach { lookup.eventsOn(it, all) } } }

        best shouldBeLessThan BUDGET_MILLIS
    }

    private fun benchmarkDefinitions(): List<EventDefinition> =
        (1..12).flatMap { month ->
            listOf(
                event(
                    "bench.persian-$month",
                    CalendarSystem.PERSIAN,
                    EventRule.Fixed(month, 10),
                    isHoliday =
                        month % 2 == 0,
                ),
                event("bench.islamic-$month", CalendarSystem.ISLAMIC, EventRule.Fixed(month, 15)),
                event(
                    "bench.monday-$month",
                    CalendarSystem.GREGORIAN,
                    EventRule.NthWeekdayOfMonth(month, Weekday.MONDAY, 2),
                ),
                event("bench.month-end-$month", CalendarSystem.GREGORIAN, EventRule.LastDayOfMonth(month)),
                event(
                    "bench.friday-$month",
                    CalendarSystem.PERSIAN,
                    EventRule.LastWeekdayOfMonth(month, Weekday.FRIDAY),
                ),
            )
        }

    private companion object {
        const val DAYS = 10_000
        const val WARM_UP_RUNS = 2
        const val MEASURED_RUNS = 5
        const val BUDGET_MILLIS = 50L
    }
}
