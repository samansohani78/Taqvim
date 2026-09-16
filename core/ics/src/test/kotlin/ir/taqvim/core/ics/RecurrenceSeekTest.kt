/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** Review I03: seeking to a day gives exactly the later occurrences, and periods stay cheap and bounded. */
class RecurrenceSeekTest {
    private val calendars: List<CalendarArithmetic> =
        listOf(GregorianCalendarSystem, PersianCalendarSystem, TabularIslamicCalendar.TYPE_II, NepaliCalendarSystem)

    private val rules: Arb<RecurrenceRule> =
        Arb.bind(
            Arb.element(Frequency.entries),
            Arb.int(1..40),
            Arb.list(Arb.element(Weekday.entries), 0..2),
            Arb.list(Arb.element((-31..-1) + (1..31)), 0..2),
            Arb.element(InvalidDatePolicy.entries),
        ) { frequency, interval, weekdays, monthDays, invalidDates ->
            // A DAILY rule takes at most one filter, so its candidates never stay empty for MAX_EMPTY_PERIODS periods
            // (the walk from the start would end there and differ from the seek on purpose).
            val daily = frequency == Frequency.DAILY
            RecurrenceRule(
                frequency = frequency,
                interval = interval,
                byDay = weekdays.distinct().map { WeekdayNum(it) },
                byMonthDay = if (daily && weekdays.isNotEmpty()) emptyList() else monthDays.distinct(),
                invalidDates = invalidDates,
            )
        }

    @Test
    fun `seeking gives the same occurrences as filtering the whole series`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.element(calendars),
                rules,
                Arb.int(2_400_000..2_470_000),
                Arb.int(0..20_000),
            ) { calendar, rule, startDay, offset ->
                val engine = RecurrenceEngine(calendar)
                val start = calendar.fromJdn(Jdn(startDay.toLong()))
                val from = Jdn(startDay.toLong() + offset)
                val sought = engine.occurrences(start, rule, from).take(SAMPLE).toList()
                sought shouldBe
                    engine
                        .occurrences(start, rule)
                        .filter { it >= from }
                        .take(SAMPLE)
                        .toList()
            }
        }

    @Test
    fun `COUNT and UNTIL keep their meaning when seeking`() {
        val engine = RecurrenceEngine(GregorianCalendarSystem)
        val start = CalendarDate(CalendarSystem.GREGORIAN, 2026, 1, 1)
        val from = LocalDate(2026, 1, 10).toJdn()
        engine.occurrences(start, RecurrenceRule(Frequency.DAILY, count = 12), from).toList() shouldBe
            (0L..2L).map { from + it }
        engine.occurrences(start, RecurrenceRule(Frequency.DAILY, until = from + 1), from).toList() shouldBe
            listOf(from, from + 1)
        engine
            .occurrences(start, RecurrenceRule(Frequency.DAILY), GregorianCalendarSystem.toJdn(start) - 5)
            .first() shouldBe GregorianCalendarSystem.toJdn(start)
    }

    @Test
    fun `huge intervals end before the last representable day`() {
        val gregorian = CalendarDate(CalendarSystem.GREGORIAN, 2026, 9, 16)
        val persian = CalendarDate(CalendarSystem.PERSIAN, 1405, 6, 25)
        val cases =
            listOf(
                GregorianCalendarSystem to gregorian,
                PersianCalendarSystem to persian,
            )
        for ((calendar, start) in cases) {
            for (frequency in Frequency.entries) {
                val occurrences =
                    RecurrenceEngine(calendar)
                        .occurrences(start, RecurrenceRule(frequency, interval = Int.MAX_VALUE))
                        .toList()
                occurrences shouldHaveAtMostSize MAX_HUGE_OCCURRENCES
                occurrences.last().value shouldBeLessThanOrEqual CalendarLimits.LAST_DAY.value
            }
        }
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a series from 1900 or year 1 reaches 2026 without walking its history`() {
        val from = LocalDate(2026, 9, 16).toJdn()
        val cases =
            listOf(
                CalendarDate(CalendarSystem.GREGORIAN, 1900, 1, 1) to RecurrenceRule(Frequency.DAILY),
                CalendarDate(CalendarSystem.GREGORIAN, 1, 1, 15) to RecurrenceRule(Frequency.MONTHLY),
                CalendarDate(CalendarSystem.PERSIAN, 1, 1, 15) to RecurrenceRule(Frequency.MONTHLY),
                CalendarDate(CalendarSystem.GREGORIAN, 1900, 1, 1) to
                    RecurrenceRule(Frequency.YEARLY, byDay = listOf(WeekdayNum(Weekday.MONDAY, 1))),
            )
        repeat(WARM_UP) { cases.forEach { (start, rule) -> first(start, rule, from) } }
        for ((start, rule) in cases) {
            val began = System.nanoTime()
            repeat(RUNS) { first(start, rule, from) }
            ((System.nanoTime() - began) / RUNS) shouldBeLessThan SEEK_BUDGET_NANOS
        }
    }

    private fun first(
        start: CalendarDate,
        rule: RecurrenceRule,
        from: Jdn,
    ): Jdn {
        val calendar = if (start.system == CalendarSystem.PERSIAN) PersianCalendarSystem else GregorianCalendarSystem
        return RecurrenceEngine(calendar).occurrences(start, rule, from).first()
    }

    private companion object {
        const val SAMPLE = 20
        const val MAX_HUGE_OCCURRENCES = 500
        const val WARM_UP = 20
        const val RUNS = 100
        const val SEEK_BUDGET_NANOS = 1_000_000L
    }
}
