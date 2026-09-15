/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1003: exception dates and overridden instances of series in any calendar. */
class SeriesInstancesTest {
    private fun days(vararg values: Long) = values.map(::Jdn)

    @Test
    fun `excluded days are removed and overrides replace their instance`() {
        val instances =
            seriesInstances(
                occurrences = days(10, 11, 12, 13, 14).asSequence(),
                excluded = setOf(Jdn(11)),
                overrides = mapOf(Jdn(13) to "moved"),
                from = Jdn(10),
                until = Jdn(14),
            )

        instances shouldBe
            listOf(
                SeriesInstance(Jdn(10), null),
                SeriesInstance(Jdn(12), null),
                SeriesInstance(Jdn(13), "moved"),
                SeriesInstance(Jdn(14), null),
            )
    }

    @Test
    fun `overridden instances outside the window are kept and overrides of other days ignored`() {
        val instances =
            seriesInstances(
                occurrences = days(10, 20, 30, 40).asSequence(),
                excluded = setOf(Jdn(30)),
                overrides = mapOf(Jdn(40) to "late", Jdn(25) to "not an occurrence", Jdn(30) to "excluded"),
                from = Jdn(15),
                until = Jdn(20),
            )

        instances shouldBe listOf(SeriesInstance(Jdn(20), null), SeriesInstance(Jdn(40), "late"))
    }

    @Test
    fun `Persian and Islamic series never yield an excluded day and override exactly their instance`(): Unit =
        runBlocking {
            val calendars = Arb.element<CalendarArithmetic>(PersianCalendarSystem, TabularIslamicCalendar.TYPE_II)
            checkAll(
                PropertyTesting.iterations,
                calendars,
                Arb.enum<Frequency>(),
                Arb.int(1400..1460),
                Arb.element(InvalidDatePolicy.entries),
                Arb.int(0..1000),
            ) { calendar, frequency, year, policy, seed ->
                val start = CalendarDate(calendar.system, year, 1 + seed % 12, 1 + seed % 29)
                val rule = RecurrenceRule(frequency, interval = 1 + seed % 3, count = 40, invalidDates = policy)
                val all = RecurrenceEngine(calendar).occurrences(start, rule).toList()
                val excluded = all.filterIndexed { index, _ -> (index * 7 + seed) % 4 == 0 }.toSet()
                val overridden = all.filterIndexed { index, _ -> (index + seed) % 5 == 0 }.associateWith { it.value }

                val instances = seriesInstances(all.asSequence(), excluded, overridden, all.first(), all.last())

                instances.none { it.original in excluded } shouldBe true
                instances.map { it.original } shouldBe all.filterNot { it in excluded }
                instances.forEach { it.override shouldBe overridden[it.original] }
            }
        }
}
