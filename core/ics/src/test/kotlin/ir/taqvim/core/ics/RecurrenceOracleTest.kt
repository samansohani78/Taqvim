/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.JDN_OF_UNIX_EPOCH
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * T-503 golden cases: 56 Gregorian rules expanded by [RecurrenceEngine] and, independently, by a day-by-day java.time
 * filter that tests each date against the RFC 5545 §3.3.10 definitions. Plus the ordering property in three calendars.
 */
class RecurrenceOracleTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val engine = RecurrenceEngine(GregorianCalendarSystem)

    private fun ruleOf(text: String): RecurrenceRule =
        requireNotNull(
            IcsValues.recurrence(ContentLine(1, "RRULE", emptyMap(), text), mutableListOf()),
        ).toRecurrenceRule()

    private fun LocalDate.jdn(): Long = toEpochDay() + JDN_OF_UNIX_EPOCH

    /** Day-by-day oracle: the start, then every later date the rule accepts, up to [limit] dates or [HORIZON_YEARS]. */
    private fun oracle(
        start: LocalDate,
        rule: RecurrenceRule,
        limit: Int,
    ): List<LocalDate> {
        val result = mutableListOf(start)
        val horizon = start.plusYears(HORIZON_YEARS)
        var day = start.plusDays(1)
        while (result.size < limit && withinBounds(day, horizon, rule)) {
            if (accepts(start, rule, day)) result += day
            day = day.plusDays(1)
        }
        return result
    }

    private fun withinBounds(
        day: LocalDate,
        horizon: LocalDate,
        rule: RecurrenceRule,
    ): Boolean = !day.isAfter(horizon) && (rule.until == null || day.jdn() <= rule.until.value)

    private fun accepts(
        start: LocalDate,
        rule: RecurrenceRule,
        day: LocalDate,
    ): Boolean =
        when (rule.frequency) {
            Frequency.DAILY -> {
                ChronoUnit.DAYS.between(start, day) % rule.interval == 0L && dailyAccepted(rule, day)
            }

            Frequency.WEEKLY -> {
                val weeks = ChronoUnit.DAYS.between(weekOf(start), weekOf(day)) / DAYS_PER_WEEK
                val weekdays = rule.byDay.map { it.weekday }.ifEmpty { listOf(weekday(start)) }
                weeks % rule.interval == 0L && weekday(day) in weekdays
            }

            Frequency.MONTHLY -> {
                val months = (day.year * MONTHS + day.monthValue) - (start.year * MONTHS + start.monthValue)
                months % rule.interval == 0 && monthDayAccepted(start, rule, day)
            }

            Frequency.YEARLY -> {
                (day.year - start.year) % rule.interval == 0 && yearDayAccepted(start, rule, day)
            }
        }

    /** BYDAY and BYMONTHDAY limit a daily rule: both must accept the day (RFC 5545 §3.3.10, table of BY parts). */
    private fun dailyAccepted(
        rule: RecurrenceRule,
        day: LocalDate,
    ): Boolean {
        val length = day.lengthOfMonth()
        val byMonthDay =
            rule.byMonthDay.isEmpty() ||
                rule.byMonthDay.any { it == day.dayOfMonth || length + it + 1 == day.dayOfMonth }
        val byDay = rule.byDay.isEmpty() || rule.byDay.any { weekday(day) == it.weekday }
        return byMonthDay && byDay
    }

    private fun monthDayAccepted(
        start: LocalDate,
        rule: RecurrenceRule,
        day: LocalDate,
    ): Boolean {
        val length = day.lengthOfMonth()
        val byMonthDay = rule.byMonthDay.any { it == day.dayOfMonth || length + it + 1 == day.dayOfMonth }
        val byDay =
            rule.byDay.any { entry ->
                weekday(day) == entry.weekday &&
                    ordinalMatches(entry.ordinal, (day.dayOfMonth - 1) / 7 + 1, -((length - day.dayOfMonth) / 7 + 1))
            }
        return combine(rule, byMonthDay, byDay) { day.dayOfMonth == start.dayOfMonth }
    }

    private fun yearDayAccepted(
        start: LocalDate,
        rule: RecurrenceRule,
        day: LocalDate,
    ): Boolean {
        val length = day.lengthOfMonth()
        val byMonthDay = rule.byMonthDay.any { it == day.dayOfMonth || length + it + 1 == day.dayOfMonth }
        val byDay =
            rule.byDay.any { entry ->
                weekday(day) == entry.weekday &&
                    ordinalMatches(
                        entry.ordinal,
                        (day.dayOfYear - 1) / 7 + 1,
                        -(
                            (day.lengthOfYear() - day.dayOfYear) /
                                7 +
                                1
                        ),
                    )
            }
        return combine(rule, byMonthDay, byDay) {
            day.monthValue == start.monthValue &&
                day.dayOfMonth == start.dayOfMonth
        }
    }

    private fun combine(
        rule: RecurrenceRule,
        byMonthDay: Boolean,
        byDay: Boolean,
        implicit: () -> Boolean,
    ): Boolean =
        when {
            rule.byMonthDay.isNotEmpty() && rule.byDay.isNotEmpty() -> byMonthDay && byDay
            rule.byMonthDay.isNotEmpty() -> byMonthDay
            rule.byDay.isNotEmpty() -> byDay
            else -> implicit()
        }

    private fun ordinalMatches(
        ordinal: Int?,
        fromStart: Int,
        fromEnd: Int,
    ): Boolean = ordinal == null || ordinal == fromStart || ordinal == fromEnd

    private fun weekOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun weekday(day: LocalDate): Weekday = Weekday.ofIsoNumber(day.dayOfWeek.value)

    @TestFactory
    fun `56 Gregorian rules agree with the day-by-day oracle`(): List<DynamicTest> =
        CASES.map { (start, text) ->
            DynamicTest.dynamicTest("$start $text") {
                val rule = ruleOf(text)
                val startDate = LocalDate.parse(start)
                val expected = oracle(startDate, rule, minOf(rule.count ?: LIMIT, LIMIT))
                val calendarStart =
                    CalendarDate(CalendarSystem.GREGORIAN, startDate.year, startDate.monthValue, startDate.dayOfMonth)
                val actual =
                    engine
                        .occurrences(calendarStart, rule)
                        .take(expected.size)
                        .map { it.value }
                        .toList()
                withClue("expected ${expected.joinToString()}") { actual shouldBe expected.map { it.jdn() } }
                expected.size shouldBeGreaterThan 1
            }
        }

    @Test
    fun `there are 56 golden cases`() {
        CASES shouldHaveSize 56
    }

    @Test
    fun `occurrences strictly increase in every calendar`(): Unit =
        runBlocking {
            val rules =
                arbitrary {
                    RecurrenceRule(
                        frequency = Arb.enum<Frequency>().bind(),
                        interval = Arb.int(1..4).bind(),
                        byDay =
                            Arb
                                .list(
                                    arbitrary { WeekdayNum(Arb.enum<Weekday>().bind(), Arb.element(ORDINALS).bind()) },
                                    0..2,
                                ).bind(),
                        byMonthDay = Arb.list(Arb.element((1..31) + (-31..-1)), 0..2).bind(),
                        invalidDates = Arb.enum<InvalidDatePolicy>().bind(),
                    )
                }
            val calendars = Arb.element(CALENDARS)
            checkAll(propertyConfig, rules, calendars, Arb.int(0..50_000)) { rule, calendar, offset ->
                val start = calendar.fromJdn(calendar.toJdn(START_ANCHOR.getValue(calendar.system)).plus(offset))
                val days =
                    RecurrenceEngine(
                        calendar,
                    ).occurrences(start, rule).take(OCCURRENCES).map { it.value }.toList()
                days.zipWithNext().forEach { (earlier, later) -> (later > earlier) shouldBe true }
            }
        }

    @Test
    fun `occurrences strictly increase at the offset range edges and with no byDay or byMonthDay`() {
        // A fixed seed permanently commits checkAll to one set of draws; pin offset 0 and 50_000 (the Arb.int(0..
        // 50_000) boundaries) together with a rule whose byDay/byMonthDay lists are both empty (the 0-length end of
        // Arb.list(..., 0..2)), crossed with every Frequency and every calendar, so these edges are never left
        // untested purely by chance.
        val frequencyCalendars =
            Frequency.entries.flatMap { frequency -> CALENDARS.map { calendar -> frequency to calendar } }
        val cases = frequencyCalendars.flatMap { fc -> listOf(0, 50_000).map { offset -> fc to offset } }
        cases.forEach { (fc, offset) ->
            val (frequency, calendar) = fc
            val rule =
                RecurrenceRule(frequency = frequency, interval = 1, byDay = emptyList(), byMonthDay = emptyList())
            val start = calendar.fromJdn(calendar.toJdn(START_ANCHOR.getValue(calendar.system)).plus(offset))
            val days =
                RecurrenceEngine(calendar)
                    .occurrences(start, rule)
                    .take(OCCURRENCES)
                    .map { it.value }
                    .toList()
            days.zipWithNext().forEach { (earlier, later) -> (later > earlier) shouldBe true }
        }
    }

    private companion object {
        const val LIMIT = 25
        const val OCCURRENCES = 40
        const val HORIZON_YEARS = 12L
        const val DAYS_PER_WEEK = 7L
        const val MONTHS = 12
        val ORDINALS = listOf(null, 1, 2, -1, -2, 5)
        val CALENDARS: List<CalendarArithmetic> =
            listOf(GregorianCalendarSystem, PersianCalendarSystem, TabularIslamicCalendar.TYPE_II)
        val START_ANCHOR =
            mapOf(
                CalendarSystem.GREGORIAN to CalendarDate(CalendarSystem.GREGORIAN, 1950, 1, 1),
                CalendarSystem.PERSIAN to CalendarDate(CalendarSystem.PERSIAN, 1330, 1, 1),
                CalendarSystem.ISLAMIC to CalendarDate(CalendarSystem.ISLAMIC, 1370, 1, 1),
            )
        val CASES =
            listOf(
                "1997-09-02" to "FREQ=DAILY;COUNT=10",
                "1997-09-02" to "FREQ=DAILY;INTERVAL=2",
                "1997-09-02" to "FREQ=DAILY;INTERVAL=10;COUNT=5",
                "2026-01-31" to "FREQ=DAILY;UNTIL=20260215",
                "1997-09-02" to "FREQ=WEEKLY;COUNT=10",
                "1997-09-02" to "FREQ=WEEKLY;INTERVAL=2",
                "1997-09-02" to "FREQ=WEEKLY;BYDAY=TU,TH;COUNT=10",
                "1997-09-01" to "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;UNTIL=19971224",
                "1997-09-02" to "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH;COUNT=8",
                "2026-03-21" to "FREQ=WEEKLY;BYDAY=SA,SU",
                "2026-03-22" to "FREQ=WEEKLY;INTERVAL=3;BYDAY=SU",
                "1997-09-05" to "FREQ=MONTHLY;BYDAY=1FR;COUNT=10",
                "1997-09-07" to "FREQ=MONTHLY;INTERVAL=2;BYDAY=1SU,-1SU;COUNT=10",
                "1997-09-22" to "FREQ=MONTHLY;BYDAY=-2MO;COUNT=6",
                "1997-09-28" to "FREQ=MONTHLY;BYMONTHDAY=-3",
                "1997-09-02" to "FREQ=MONTHLY;BYMONTHDAY=2,15;COUNT=10",
                "1997-09-30" to "FREQ=MONTHLY;BYMONTHDAY=1,-1;COUNT=10",
                "1997-09-10" to "FREQ=MONTHLY;INTERVAL=18;BYMONTHDAY=10,11,12,13,14,15;COUNT=10",
                "1997-09-02" to "FREQ=MONTHLY;INTERVAL=2;BYDAY=TU",
                "2026-01-31" to "FREQ=MONTHLY",
                "2026-01-31" to "FREQ=MONTHLY;INTERVAL=3",
                "2026-03-13" to "FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13",
                "2026-01-01" to "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYMONTHDAY=1",
                "2026-01-05" to "FREQ=MONTHLY;BYDAY=-1FR",
                "2026-02-28" to "FREQ=MONTHLY;BYMONTHDAY=29,30,31",
                "1997-06-10" to "FREQ=YEARLY;COUNT=10",
                "2024-02-29" to "FREQ=YEARLY",
                "1997-01-01" to "FREQ=YEARLY;INTERVAL=2",
                "2026-03-21" to "FREQ=YEARLY;BYMONTHDAY=21",
                "2026-01-01" to "FREQ=YEARLY;BYDAY=20MO",
                "2026-01-01" to "FREQ=YEARLY;BYDAY=-1SU",
                "2026-01-01" to "FREQ=YEARLY;BYDAY=1MO,-1FR",
                "2026-01-13" to "FREQ=YEARLY;BYMONTHDAY=13;BYDAY=FR",
                "2026-01-01" to "FREQ=YEARLY;BYMONTHDAY=-1;INTERVAL=2",
                "2026-06-15" to "FREQ=YEARLY;UNTIL=20300615",
                "2026-12-31" to "FREQ=DAILY;COUNT=3",
                "2026-03-01" to "FREQ=WEEKLY;BYDAY=MO;COUNT=5",
                "2026-03-02" to "FREQ=MONTHLY;BYDAY=2MO,4MO",
                "2026-01-29" to "FREQ=MONTHLY;BYMONTHDAY=29",
                "2026-09-13" to "FREQ=DAILY;INTERVAL=7;UNTIL=20261231",
                "2026-09-13" to "FREQ=WEEKLY;INTERVAL=4;BYDAY=TU,SA;COUNT=12",
                "2026-09-13" to "FREQ=MONTHLY;INTERVAL=5;BYMONTHDAY=-1,-7",
                "2026-09-13" to "FREQ=YEARLY;BYDAY=3TH",
                "2026-09-13" to "FREQ=YEARLY;INTERVAL=4;BYMONTHDAY=1",
                "2000-02-29" to "FREQ=YEARLY;INTERVAL=4",
                "2026-11-01" to "FREQ=MONTHLY;BYDAY=SU;COUNT=12",
                "2026-04-30" to "FREQ=MONTHLY;BYMONTHDAY=31;COUNT=6",
                "2026-07-04" to "FREQ=WEEKLY;INTERVAL=2;COUNT=6",
                "2026-10-31" to "FREQ=YEARLY;BYMONTHDAY=31;COUNT=10",
                "2026-01-01" to "FREQ=YEARLY;BYDAY=1MO,1TU,1WE;COUNT=9",
                "2026-09-14" to "FREQ=DAILY;BYDAY=MO;COUNT=3",
                "2026-09-14" to "FREQ=DAILY;BYDAY=SA,SU;COUNT=10",
                "2026-01-15" to "FREQ=DAILY;BYMONTHDAY=15,-1;COUNT=12",
                "2026-01-31" to "FREQ=DAILY;BYMONTHDAY=-1;COUNT=14",
                "2026-02-13" to "FREQ=DAILY;BYDAY=FR;BYMONTHDAY=13",
                "2026-09-14" to "FREQ=DAILY;INTERVAL=2;BYDAY=MO,WE;COUNT=8",
            )
    }
}
