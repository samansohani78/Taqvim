/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1003 (P/U): exception days and overrides in the day assembly of personal events (T-305). */
class PersonalExpansionTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig =
        PropTestConfig(seed = 20_260_920L, iterations = minOf(PropertyTesting.iterations, ITERATIONS))

    private val calendars = CalendarProvider.DEFAULT
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))

    private fun entity(
        start: Jdn,
        calendar: CalendarSystem = CalendarSystem.PERSIAN,
        length: Long = 0,
    ) = PersonalEventEntity(
        id = 1,
        title = "Event",
        calendarSystem = calendar,
        startJdn = start.value,
        startMinute = 600,
        endJdn = start.value + length,
        endMinute = 660,
        timeZoneId = "Asia/Tehran",
        colorArgb = 5,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )

    @Test
    fun `exceptions and cancelled overrides remove occurrences and overrides move and restyle theirs`() {
        val moved = EventOverrideEntity(1, nowruz.value + 2, "Moved", "n", nowruz.value + 20, null, nowruz.value + 21)
        val styledDay = nowruz.value + 4
        val restyled = EventOverrideEntity(1, styledDay, "Styled", "", styledDay, 900, styledDay, 960, 9)
        val goneDay = nowruz.value + 3
        val gone = EventOverrideEntity(1, goneDay, "Gone", "", goneDay, null, goneDay, cancelled = true)
        val record =
            PersonalEventRecord(
                entity(nowruz),
                RecurrenceRule(Frequency.DAILY, count = 6),
                exceptions = setOf(nowruz + 1),
                overrides = listOf(moved, restyled, gone),
            )

        val week = PersonalExpansion.expand(record, nowruz..nowruz + 6, calendars)
        week.map { Triple(it.days.start - nowruz, it.title, it.startMinute) } shouldBe
            listOf(Triple(0L, "Event", 600), Triple(4L, "Styled", 900), Triple(5L, "Event", 600))
        week.map { it.colorArgb } shouldBe listOf(5, 9, 5)

        val later = PersonalExpansion.expand(record, nowruz + 21..nowruz + 21, calendars).single()
        listOf(later.title, later.notes, later.days, later.startMinute, later.colorArgb, later.recurring) shouldBe
            listOf("Moved", "n", nowruz + 20..nowruz + 21, null, 5, true)
    }

    @Test
    fun `recurring occurrences carry their original day, also when moved, and one-off events do not`() {
        val moved = EventOverrideEntity(1, nowruz.value + 2, "Moved", "", nowruz.value + 20, null, nowruz.value + 20)
        val rule = RecurrenceRule(Frequency.DAILY, count = 3)
        val record = PersonalEventRecord(entity(nowruz), rule, overrides = listOf(moved))
        val occurrences = PersonalExpansion.expand(record, nowruz..nowruz + 20, calendars)
        occurrences.map { it.originalDay } shouldBe listOf(nowruz, nowruz + 1, nowruz + 2)
        occurrences.last().days.start shouldBe nowruz + 20
        occurrences.map { it.itemId } shouldBe
            listOf(nowruz, nowruz + 1, nowruz + 2).map { "1${PersonalOccurrence.OCCURRENCE_SEPARATOR}${it.value}" }

        val oneOff = PersonalEventRecord(entity(nowruz), recurrence = null)
        val single = PersonalExpansion.expand(oneOff, nowruz..nowruz, calendars).single()
        single.originalDay shouldBe null
        single.itemId shouldBe "1"
    }

    private fun checkNeverYieldsExceptedInstance(
        system: CalendarSystem,
        frequency: Frequency,
        seed: Int,
    ) {
        val calendar = requireNotNull(calendars.calendarFor(system))
        val start = nowruz + seed % 400
        val rule = RecurrenceRule(frequency, interval = 1 + seed % 2, count = 30)
        val all = RecurrenceEngine(calendar).occurrences(calendar.fromJdn(start), rule).toList()
        val excluded = all.filterIndexed { index, _ -> (index + seed) % 4 == 0 }.toSet()
        val overridden = all.filterIndexed { index, _ -> (index + seed) % 4 == 1 }
        val overrides =
            overridden.map { day ->
                EventOverrideEntity(1, day.value, "Override", "", day.value + 2, null, day.value + 2)
            }
        val record = PersonalEventRecord(entity(start, system, seed % 3L), rule, excluded, overrides)

        val occurrences = PersonalExpansion.expand(record, all.first()..all.last() + 2, calendars)

        val (replaced, plain) = occurrences.partition { it.title == "Override" }
        plain.none { it.days.start in excluded } shouldBe true
        plain.map { it.days.start } shouldBe all.filter { it !in excluded && it !in overridden }
        replaced.map { it.days.start } shouldBe overridden.filter { it !in excluded }.map { it + 2 }
    }

    @Test
    fun `Persian and Islamic series never yield an excepted instance and overrides replace exactly theirs`(): Unit =
        runBlocking {
            checkAll(
                propertyConfig,
                Arb.element(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC),
                Arb.enum<Frequency>(),
                Arb.int(0..1000),
            ) { system, frequency, seed -> checkNeverYieldsExceptedInstance(system, frequency, seed) }
        }

    @Test
    fun `every frequency and calendar system is checked at both ends of the seed range`() {
        // A fixed seed permanently commits checkAll to one draw per input; pin the seed boundaries (0 and 1000, which
        // drive the interval, the exclusion pattern and the event length) crossed with every Frequency so a rare
        // enum value is never left untested purely by chance.
        val systemFrequencies =
            listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC).flatMap { system ->
                Frequency.entries.map { frequency -> system to frequency }
            }
        systemFrequencies
            .flatMap { pair -> listOf(0, 1000).map { seed -> Triple(pair.first, pair.second, seed) } }
            .forEach { (system, frequency, seed) -> checkNeverYieldsExceptedInstance(system, frequency, seed) }
    }

    private companion object {
        const val ITERATIONS = 200
    }
}
