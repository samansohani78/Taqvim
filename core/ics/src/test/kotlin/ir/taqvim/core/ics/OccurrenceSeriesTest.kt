/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** Review I01 / ADR-0035: the shared pipeline gives what expanding the whole series from its start gives. */
class OccurrenceSeriesTest {
    private val calendars: List<CalendarArithmetic> =
        listOf(GregorianCalendarSystem, PersianCalendarSystem, TabularIslamicCalendar.TYPE_II)
    private val start = CalendarDate(CalendarSystem.GREGORIAN, 1990, 1, 31)

    @Test
    fun `instances equal the series expanded from its start and filtered to the days`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.element(calendars),
                Arb.element(Frequency.entries),
                Arb.int(1..5),
                Arb.long(0L..20_000L),
                Arb.long(0L..6L),
            ) { calendar, frequency, interval, offset, length ->
                val begin = calendar.fromJdn(Jdn(START_JDN))
                val rule = RecurrenceRule(frequency, interval)
                val all = RecurrenceEngine(calendar).occurrences(begin, rule).takeWhile { it <= Jdn(START_JDN + LIMIT) }
                val from = Jdn(START_JDN + offset)
                val until = from + WINDOW
                val excluded = all.drop(3).take(2).toSet()
                val moved =
                    all
                        .drop(offset.toInt() % 50)
                        .firstOrNull()
                        ?.let { mapOf(it to "moved") }
                        .orEmpty()
                val series = OccurrenceSeries(calendar, begin, rule, length, excluded, moved)

                series.instances(from, until) shouldBe
                    seriesInstances(all, excluded, moved, from - length, until)
            }
        }

    @Test
    fun `a one-off series has its single day`() {
        val series = OccurrenceSeries<String>(GregorianCalendarSystem, start, rule = null, lengthDays = 2)
        val first = series.first

        series.starts().toList() shouldBe listOf(first)
        series.starts(first + 1).toList() shouldBe emptyList()
        series.isOccurrence(first) shouldBe true
        series.isOccurrence(first + 1) shouldBe false
        series.instances(first + 2, first + 5).map { it.original } shouldBe listOf(first)
        series.instances(first + 3, first + 5) shouldBe emptyList()
        series.copy(excluded = setOf(first)).instances(first, first) shouldBe emptyList()
    }

    @Test
    fun `occurrence days follow the rule and invalid series are rejected`() {
        val monthly = OccurrenceSeries<String>(GregorianCalendarSystem, start, RecurrenceRule(Frequency.MONTHLY))
        val first = monthly.first

        monthly.isOccurrence(first + 28) shouldBe false
        monthly.isOccurrence(monthly.starts(first + 1).first()) shouldBe true
        shouldThrow<IllegalArgumentException> { OccurrenceSeries<String>(PersianCalendarSystem, start, null) }
        shouldThrow<IllegalArgumentException> { OccurrenceSeries<String>(GregorianCalendarSystem, start, null, -1) }
    }

    private companion object {
        const val START_JDN = 2_447_893L
        const val LIMIT = 30_000L
        const val WINDOW = 60
    }
}
