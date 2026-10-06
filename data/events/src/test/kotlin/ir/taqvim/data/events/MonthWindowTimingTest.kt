/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.longs.shouldBeLessThan
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.TimingTest
import ir.taqvim.data.database.PersonalEventEntity
import kotlin.system.measureNanoTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Budget of the month pager's event load (ADR-0018 addendum): one 42-day grid page assembled from the whole dataset
 * plus a heavy personal calendar, which is what [EventsRepository.days] answers for every page the pager keeps.
 *
 * The pager holds three pages and the day-details pane asks for a fourth, so this runs four times per swipe on
 * `Dispatchers.Default`; a page that costs more than a frame is a stutter the user sees.
 */
class MonthWindowTimingTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = now
        }
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))
    private val page = (nowruz - GRID_LEAD)..(nowruz - GRID_LEAD + (GRID_DAYS - 1))

    private val personal = MutableStateFlow(personalEvents())
    private val repository =
        EventsRepository(
            settings = MutableStateFlow(settings()),
            inputs =
                EventInputs(
                    personal = PersonalEventsSource { personal },
                    device = DeviceEventsSource { MutableStateFlow(emptyList()) },
                    ics = IcsEventsSource { MutableStateFlow(emptyList()) },
                ),
            clock = clock,
            zones = flowOf(tehran),
            computeDispatcher = Dispatchers.Unconfined,
        )

    @Test
    @Tag(TimingTest.TAG)
    fun `a month page of the whole dataset and 200 personal events loads within a frame`(): Unit =
        runTest {
            repeat(WARM_UP_RUNS) { repository.days(page).first() }

            val best = (1..MEASURED_RUNS).minOf { measureNanoTime { repository.days(page).first() } }
            val micros = best / NANOS_PER_MICRO

            println("T-305 month page ($GRID_DAYS days, $PERSONAL_EVENTS personal) best of $MEASURED_RUNS: $micros us")
            micros shouldBeLessThan TimingTest.budget(BUDGET_MICROS)
        }

    /** Every source and category on, with the official Iranian months computed, as a default install reads them. */
    private fun settings() =
        EventsSettings(
            preferences =
                EventPreferences(
                    enabledSources = EventSource.entries.toSet(),
                    homeTimeZone = tehran,
                    islamicVariant = IslamicVariant.IRAN_OFFICIAL,
                ),
            weekend = setOf(Weekday.FRIDAY),
            hijriOffset = null,
        )

    /** A heavy personal calendar: every other event repeats weekly, so the page expands as well as filters. */
    private fun personalEvents(): List<PersonalEventRecord> =
        (1..PERSONAL_EVENTS).map { index ->
            val start = nowruz - (index % PERSONAL_SPREAD_DAYS).toLong()
            PersonalEventRecord(
                event =
                    PersonalEventEntity(
                        id = index.toLong(),
                        title = "personal $index",
                        calendarSystem = CalendarSystem.PERSIAN,
                        startJdn = start.value,
                        startMinute = index % MINUTES_PER_DAY,
                        endMinute = (index % MINUTES_PER_DAY) + HOUR_MINUTES,
                        endJdn = start.value,
                        timeZoneId = tehran.id,
                        createdAtEpochMillis = 0,
                        updatedAtEpochMillis = 0,
                    ),
                recurrence = if (index % 2 == 0) RecurrenceRule(Frequency.WEEKLY) else null,
            )
        }

    private companion object {
        const val GRID_DAYS = 42
        const val GRID_LEAD = 3L
        const val PERSONAL_EVENTS = 200
        const val PERSONAL_SPREAD_DAYS = 400
        const val MINUTES_PER_DAY = 1440
        const val HOUR_MINUTES = 60
        const val WARM_UP_RUNS = 5
        const val MEASURED_RUNS = 15
        const val NANOS_PER_MICRO = 1_000L

        /** One frame at 60 Hz, which is the pager's whole budget for the four pages it loads per swipe. */
        const val BUDGET_MICROS = 16_000L
    }
}
