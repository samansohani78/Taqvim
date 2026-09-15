/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
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
    fun `Persian and Islamic series never yield an excepted instance and overrides replace exactly theirs`(): Unit =
        runBlocking {
            checkAll(
                minOf(PropertyTesting.iterations, ITERATIONS),
                Arb.element(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC),
                Arb.enum<Frequency>(),
                Arb.int(0..1000),
            ) { system, frequency, seed ->
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
        }

    private companion object {
        const val ITERATIONS = 200
    }
}
