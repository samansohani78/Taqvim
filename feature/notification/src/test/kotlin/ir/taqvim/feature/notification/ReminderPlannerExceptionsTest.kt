/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1001 with T-1003: exception days and cancelled occurrences get no reminder; changed ones use their own start. */
class ReminderPlannerExceptionsTest {
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val lastDay = LocalDate(2026, 3, 1).toJdn() + 400

    @Test
    fun `excepted and cancelled occurrences are skipped and overridden ones use their day, time and title`() {
        val start = ReminderFixtures.persian(1405, 1, 1)
        val first = ReminderFixtures.jdnOf(start)
        val weekly =
            ReminderFixtures
                .event(
                    start,
                    startMinute = MinuteOfDay.of(9, 0),
                    recurrence = RecurrenceRule(Frequency.WEEKLY, count = 4),
                    reminders = listOf(ReminderRule(1, 30)),
                ).copy(
                    exceptions = setOf(first + 7),
                    overrides =
                        listOf(
                            ReminderOverride(first + 14, first + 15, MinuteOfDay.of(18, 0), "Moved"),
                            ReminderOverride(first + 21, first + 21, null, "Event 1", cancelled = true),
                        ),
                )
        val setup = ReminderFixtures.setup(listOf(weekly))

        val planned = ReminderPlanner.upcoming(now, setup)

        planned.map { Triple(it.occurrence, it.title, it.at) } shouldBe
            listOf(
                Triple(first, "Event 1", Instant.parse("2026-03-21T05:00:00Z")),
                Triple(first + 15, "Moved", Instant.parse("2026-04-05T14:00:00Z")),
            )
        planned.forEach { ReminderPlanner.at(it.alarmSourceId, it.at, setup) shouldBe it }
    }

    @Test
    fun `an override moved all day falls back to the all-day reminder time`() {
        val start = ReminderFixtures.persian(1405, 1, 1)
        val first = ReminderFixtures.jdnOf(start)
        val event =
            ReminderFixtures
                .event(start, startMinute = MinuteOfDay.of(9, 0))
                .copy(overrides = listOf(ReminderOverride(first, first + 2, null, "All day")))

        ReminderPlanner.upcoming(now, ReminderFixtures.setup(listOf(event))).map { it.at } shouldBe
            listOf(Instant.parse("2026-03-23T05:30:00Z"))
    }

    @Test
    fun `Persian and Islamic series never remind on an excepted or cancelled day`(): Unit =
        runBlocking {
            checkAll(
                minOf(PropertyTesting.iterations, ITERATIONS),
                Arb.element(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC),
                Arb.enum<Frequency>(),
                Arb.int(0..1000),
            ) { system, frequency, seed ->
                val calendar = requireNotNull(CalendarProvider.DEFAULT.calendarFor(system))
                val start = calendar.fromJdn(ReminderFixtures.NOWRUZ_1405 + seed % 60)
                val rule = RecurrenceRule(frequency, count = 30)
                val all = RecurrenceEngine(calendar).occurrences(start, rule).toList()
                val excluded = all.filterIndexed { index, _ -> (index + seed) % 3 == 0 }.toSet()
                val cancelled =
                    all
                        .filterIndexed { index, _ -> (index + seed) % 3 == 1 && index % 2 == 0 }
                        .map { ReminderOverride(it, it, null, "Cancelled", cancelled = true) }
                val event =
                    ReminderFixtures.event(start, recurrence = rule).copy(exceptions = excluded, overrides = cancelled)

                val planned = ReminderPlanner.upcoming(now, ReminderFixtures.setup(listOf(event)))

                val removed = excluded + cancelled.map { it.original }
                planned.map { it.occurrence } shouldBe all.filter { it !in removed && it < lastDay }
            }
        }

    private companion object {
        const val ITERATIONS = 200
    }
}
