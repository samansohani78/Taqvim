/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/** T-1001 (P/U): when personal reminders are due, in every calendar and across daylight-saving changes. */
class ReminderPlannerTest {
    private val from = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds()
    private val until = Instant.parse("2027-01-01T00:00:00Z").toEpochMilliseconds()
    private val iterations = minOf(PropertyTesting.iterations, ITERATIONS)
    private val zones = listOf("Asia/Tehran", "Europe/Berlin", "America/Los_Angeles")
    private val losAngeles = TimeZone.of("America/Los_Angeles")

    @Test
    fun `reminders are after now, ordered, once per occurrence and found again at their instant`(): Unit =
        runBlocking {
            checkAll(iterations, Arb.long(from until until), Arb.long()) { millis, seed ->
                val now = Instant.fromEpochMilliseconds(millis)
                val events = randomEvents(Random(seed))
                val setup = ReminderFixtures.setup(events)

                val planned = ReminderPlanner.upcoming(now, setup)

                planned.all { it.at > now && it.at <= now + ReminderPlanner.HORIZON_DAYS.days } shouldBe true
                planned.zipWithNext().all { (a, b) -> a.at <= b.at } shouldBe true
                planned.map { it.key }.distinct().size shouldBe planned.size
                (planned.size <= ReminderPlanner.MAX_REMINDERS) shouldBe true
                planned.forEach { reminder ->
                    ReminderPlanner.at(reminder.alarmSourceId, reminder.at, setup) shouldBe reminder
                    startsOnItsDay(reminder, events, setup.zone) shouldBe true
                }
            }
        }

    /** The reminder plus its lead time is the event's start on the occurrence day (all-day events at 09:00). */
    private fun startsOnItsDay(
        reminder: PlannedReminder,
        events: List<ReminderEvent>,
        deviceZone: TimeZone,
    ): Boolean {
        val event = events.single { it.id.toString() == reminder.target }
        val rule = event.reminders.single { it.id == reminder.sourceId }
        val zone = if (event.startMinute == null) deviceZone else TimeZone.of(event.timeZoneId)
        val start = (reminder.at + rule.minutesBefore.minutes).toLocalDateTime(zone)
        val time = event.startMinute ?: ReminderSetup.DEFAULT_ALL_DAY_TIME
        return start.date.toJdn() == reminder.occurrence && start.time == LocalTime(time.hour, time.minute)
    }

    @Test
    fun `a yearly event on 30 Esfand follows its invalid-day policy in a common year`() {
        val leap =
            (1395..1420).first {
                PersianCalendarSystem.isLeapYear(it) &&
                    !PersianCalendarSystem.isLeapYear(it + 1)
            }
        val common = leap + 1
        val now = ReminderFixtures.jdnOf(ReminderFixtures.persian(common, 1, 15)).toLocalDate().atStartOfDayIn(TEHRAN)

        fun occurrences(policy: InvalidDatePolicy): List<Jdn> {
            val rule = RecurrenceRule(Frequency.YEARLY, invalidDates = policy)
            val event = ReminderFixtures.event(ReminderFixtures.persian(leap, 12, 30), recurrence = rule)
            return ReminderPlanner.upcoming(now, ReminderFixtures.setup(listOf(event))).map { it.occurrence }
        }

        occurrences(InvalidDatePolicy.SKIP).shouldBeEmpty()
        occurrences(InvalidDatePolicy.NEXT_DAY) shouldBe
            listOf(ReminderFixtures.jdnOf(ReminderFixtures.persian(common + 1, 1, 1)))
        occurrences(InvalidDatePolicy.LAST_DAY_OF_MONTH) shouldBe
            listOf(ReminderFixtures.jdnOf(ReminderFixtures.persian(common, 12, 29)))
    }

    @Test
    fun `a monthly Islamic event repeats on the first day of each Islamic month`() {
        val islamic = requireNotNull(CalendarProvider.DEFAULT.calendarFor(CalendarSystem.ISLAMIC))
        val start = CalendarDate(CalendarSystem.ISLAMIC, 1447, 9, 1)
        val now = islamic.toJdn(start).toLocalDate().atStartOfDayIn(TEHRAN) - 1.days
        val event = ReminderFixtures.event(start, recurrence = RecurrenceRule(Frequency.MONTHLY))

        val months =
            ReminderPlanner
                .upcoming(
                    now,
                    ReminderFixtures.setup(listOf(event)),
                ).map { islamic.fromJdn(it.occurrence) }

        (months.size >= MONTHS_IN_A_YEAR) shouldBe true
        months.all { it.day == 1 } shouldBe true
        months.zipWithNext().all { (a, b) ->
            b.year * MONTHS_IN_A_YEAR + b.month ==
                a.year * MONTHS_IN_A_YEAR + a.month + 1
        } shouldBe
            true
        months.first() shouldBe start
    }

    @Test
    fun `timed reminders keep their local time across a daylight-saving change`() {
        val rule = RecurrenceRule(Frequency.DAILY)
        val start = CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 6)
        val event =
            ReminderFixtures.event(
                start,
                startMinute = MinuteOfDay.of(9, 30),
                zone = "America/Los_Angeles",
                recurrence = rule,
                reminders = listOf(ReminderRule(1, 30)),
            )
        val planned =
            ReminderPlanner
                .upcoming(
                    Instant.parse("2026-03-06T00:00:00Z"),
                    ReminderFixtures.setup(listOf(event)),
                ).take(4)

        planned.map { it.at.toLocalDateTime(losAngeles) } shouldBe
            (6..9).map { LocalDateTime(2026, 3, it, 9, 0) }
        (planned[2].at - planned[1].at) shouldBe 23.hours

        val gap = event.copy(startMinute = MinuteOfDay.of(2, 30), recurrence = null, start = start.copy(day = 8))
        ReminderPlanner
            .upcoming(Instant.parse("2026-03-06T00:00:00Z"), ReminderFixtures.setup(listOf(gap)))
            .single()
            .at
            .toLocalDateTime(losAngeles) shouldBe LocalDateTime(2026, 3, 8, 3, 0)
    }

    @Test
    fun `all-day reminders sound at the all-day time in the device zone`() {
        val berlin = TimeZone.of("Europe/Berlin")
        val event =
            ReminderFixtures.event(
                CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 28),
                recurrence = RecurrenceRule(Frequency.DAILY, count = 3),
                reminders = listOf(ReminderRule(1, 60)),
            )
        val setup = ReminderFixtures.setup(listOf(event), zone = berlin).copy(allDayTime = MinuteOfDay.of(8, 0))

        ReminderPlanner.upcoming(Instant.parse("2026-03-27T00:00:00Z"), setup).map { it.at } shouldBe
            (28..30).map { LocalDate(2026, 3, it).atTime(7, 0).toInstant(berlin) }
    }

    @Test
    fun `unavailable calendars, invalid starts, past events and the current instant are not upcoming`() {
        val now = Instant.parse("2026-09-13T05:30:00Z")
        val nepali = ReminderFixtures.event(CalendarDate(CalendarSystem.NEPALI, 2083, 5, 28))
        val invalid = ReminderFixtures.event(ReminderFixtures.persian(1405, 13, 1), id = 2)
        val past = ReminderFixtures.event(ReminderFixtures.persian(1405, 1, 1), id = 3)
        val today = now.toLocalDateTime(TEHRAN)
        val exact =
            ReminderFixtures.event(
                CalendarDate(CalendarSystem.GREGORIAN, today.date.year, today.date.month.ordinal + 1, today.date.day),
                id = 4,
                startMinute = MinuteOfDay.of(today.hour, today.minute),
            )

        ReminderPlanner.upcoming(now, ReminderFixtures.setup(listOf(nepali, invalid, past, exact))).shouldBeEmpty()
        // A reminder due exactly now is not upcoming any more, but the alarm firing at that instant still finds it.
        ReminderPlanner.at(4, now, ReminderFixtures.setup(listOf(exact)))?.at shouldBe now
        ReminderPlanner.at(3, now, ReminderFixtures.setup(listOf(past))) shouldBe null
    }

    @Test
    fun `only the nearest reminders are handed to the scheduler`() {
        val event =
            ReminderFixtures.event(
                CalendarDate(CalendarSystem.GREGORIAN, 2026, 1, 1),
                recurrence = RecurrenceRule(Frequency.DAILY),
                reminders = listOf(ReminderRule(1, 0), ReminderRule(2, 60)),
            )
        val setup = ReminderFixtures.setup(listOf(event))
        val now = Instant.parse("2026-02-01T00:00:00Z")

        val planned = ReminderPlanner.upcoming(now, setup)

        planned.size shouldBe ReminderPlanner.MAX_REMINDERS
        // Two reminders a day (09:00 and 08:00) from 1 February: the 50th is the 09:00 reminder of 25 February.
        planned.last().at shouldBe LocalDate(2026, 2, 25).atTime(9, 0).toInstant(TEHRAN)
        ReminderPlanner.at(1, LocalDate(2026, 12, 1).atTime(9, 0).toInstant(TEHRAN), setup)?.sourceId shouldBe 1
    }

    private fun randomEvents(random: Random): List<ReminderEvent> =
        (1..random.nextInt(1, MAX_EVENTS + 1)).map { index -> randomEvent(random, index) }

    private fun randomEvent(
        random: Random,
        index: Int,
    ): ReminderEvent {
        val system = SYSTEMS[random.nextInt(SYSTEMS.size)]
        val calendar = requireNotNull(CalendarProvider.DEFAULT.calendarFor(system))
        val start = calendar.fromJdn(BASE + random.nextLong(-SPREAD_DAYS, SPREAD_DAYS))
        val recurrence =
            if (random.nextInt(RECURRING_ODDS) == 0) {
                null
            } else {
                RecurrenceRule(
                    Frequency.entries[random.nextInt(Frequency.entries.size)],
                    interval = random.nextInt(1, MAX_INTERVAL + 1),
                    invalidDates = InvalidDatePolicy.entries[random.nextInt(InvalidDatePolicy.entries.size)],
                )
            }
        val reminders =
            (1..random.nextInt(1, MAX_RULES + 1)).map {
                ReminderRule(index * RULE_ID_BASE + it, LEADS[random.nextInt(LEADS.size)])
            }
        return ReminderFixtures.event(
            start,
            id = index.toLong(),
            startMinute = randomMinute(random),
            zone = zones[random.nextInt(zones.size)],
            recurrence = recurrence,
            reminders = reminders,
        )
    }

    /** No time at all (all-day), or a time outside 02:00–03:00, the hour daylight-saving changes may skip. */
    private fun randomMinute(random: Random): MinuteOfDay? =
        when (random.nextInt(3)) {
            0 -> null
            1 -> MinuteOfDay(random.nextInt(0, DST_HOUR_START))
            else -> MinuteOfDay(random.nextInt(DST_HOUR_END, MinuteOfDay.MINUTES_PER_DAY))
        }

    private companion object {
        const val ITERATIONS = 100
        const val MONTHS_IN_A_YEAR = 12
        const val MAX_EVENTS = 3
        const val MAX_RULES = 2
        const val MAX_INTERVAL = 3
        const val RECURRING_ODDS = 5
        const val RULE_ID_BASE = 10L
        const val SPREAD_DAYS = 500L
        const val DST_HOUR_START = 120
        const val DST_HOUR_END = 180
        val TEHRAN: TimeZone = ReminderFixtures.TEHRAN
        val BASE: Jdn = LocalDate(2026, 6, 1).toJdn()
        val SYSTEMS = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC)
        val LEADS = listOf(0, 15, 60, 1_440, ReminderRule.MAX_MINUTES_BEFORE)
    }
}
