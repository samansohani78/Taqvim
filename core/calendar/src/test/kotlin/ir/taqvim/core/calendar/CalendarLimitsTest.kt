/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-805/T-801: the days, years and month distances screens offer, and direct month arithmetic at any distance. */
class CalendarLimitsTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val calendars =
        listOf(
            GregorianCalendarSystem,
            PersianCalendarSystem,
            TabularIslamicCalendar.TYPE_I,
            TabularIslamicCalendar.TYPE_II,
            IranIslamicCalendar(),
            UmmAlQuraCalendar,
        )

    /** Persian arithmetic without a fixed month count, so month arithmetic takes the year-by-year path. */
    private object SteppingPersian : CalendarArithmetic by PersianCalendarSystem {
        override val monthsPerYear: Int? = null
    }

    private val today = LocalDate(2026, 9, 13).toJdn()

    @Test
    fun `the limits are the days a LocalDate holds`() {
        CalendarLimits.FIRST_DAY.toLocalDate() shouldBe LocalDate(-999_999_999, 1, 1)
        CalendarLimits.LAST_DAY.toLocalDate() shouldBe LocalDate(999_999_999, 12, 31)
        shouldThrow<IllegalArgumentException> { (CalendarLimits.FIRST_DAY - 1).toLocalDate() }
        shouldThrow<IllegalArgumentException> { (CalendarLimits.LAST_DAY + 1).toLocalDate() }
        CalendarLimits.days.start shouldBe CalendarLimits.FIRST_DAY
        CalendarLimits.days.endInclusive shouldBe CalendarLimits.LAST_DAY
        CalendarLimits.years(GregorianCalendarSystem) shouldBe -999_999_999..999_999_999
    }

    @Test
    fun `every calendar offers exactly the whole years inside the limits`() {
        calendars.forEach { calendar ->
            val years = CalendarLimits.years(calendar)
            val start = calendar.toJdn(calendar.date(years.first, 1, 1))
            start shouldBeGreaterThanOrEqualTo CalendarLimits.FIRST_DAY
            yearEnd(calendar, years.last) shouldBeLessThanOrEqualTo CalendarLimits.LAST_DAY
            calendar.toJdn(calendar.date(years.first - 1, 1, 1)) shouldBeLessThan CalendarLimits.FIRST_DAY
            (yearEnd(calendar, years.last + 1) > CalendarLimits.LAST_DAY) shouldBe true
            listOf(CalendarLimits.FIRST_DAY, CalendarLimits.LAST_DAY).forEach { day ->
                calendar.toJdn(calendar.fromJdn(day)) shouldBe day
            }
            calendar.monthsPerYear shouldBe 12
            listOf(years.first, -1, 0, 1405, years.last).forEach { calendar.monthsInYear(it) shouldBe 12 }
        }
    }

    @Test
    fun `direct month arithmetic equals stepping year by year`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(-3_000..3_000), Arb.int(1..12), Arb.int(1..31)) { y, m, d ->
                val date = persian(y, m, d.coerceAtMost(PersianCalendarSystem.monthLength(y, m)))
                val shift = Math.floorMod(y * 31 + m * 7 + d, 4_801) - 2_400
                val direct = PersianCalendarSystem.addMonths(date, shift)
                SteppingPersian.addMonths(date, shift) shouldBe direct
                val target = persian(direct.year, direct.month, 1 + Math.floorMod(d * 13, 28))
                PersianCalendarSystem.monthsBetween(date, target) shouldBe SteppingPersian.monthsBetween(date, target)
            }
        }

    @Test
    fun `direct month arithmetic equals stepping year by year at the property's own range edges`() {
        listOf(-3_000, 3_000).forEach { y ->
            listOf(1 to 1, 12 to 31).forEach { (m, d) ->
                val date = persian(y, m, d.coerceAtMost(PersianCalendarSystem.monthLength(y, m)))
                val shift = Math.floorMod(y * 31 + m * 7 + d, 4_801) - 2_400
                val direct = PersianCalendarSystem.addMonths(date, shift)
                SteppingPersian.addMonths(date, shift) shouldBe direct
                val target = persian(direct.year, direct.month, 1 + Math.floorMod(d * 13, 28))
                PersianCalendarSystem.monthsBetween(date, target) shouldBe SteppingPersian.monthsBetween(date, target)
            }
        }
    }

    @Test
    fun `month arithmetic reaches every Int month offset directly`() {
        val start = persian(1405, 6, 1)
        val far = CalendarLimits.MAX_MONTH_OFFSET
        val ahead = PersianCalendarSystem.addMonths(start, far)
        PersianCalendarSystem.monthsBetween(start, ahead) shouldBe far
        PersianCalendarSystem.monthsBetween(ahead, start) shouldBe -far
        PersianCalendarSystem.addMonths(ahead, -far) shouldBe start
        PersianCalendarSystem.addMonths(start, Int.MIN_VALUE).year shouldBe 1405 + Math.floorDiv(5L + Int.MIN_VALUE, 12)
        val years = CalendarLimits.years(PersianCalendarSystem)
        shouldThrow<ArithmeticException> {
            PersianCalendarSystem.monthsBetween(persian(years.first, 1, 1), persian(years.last, 1, 1))
        }
        shouldThrow<ArithmeticException> { PersianCalendarSystem.addMonths(persian(Int.MAX_VALUE, 12, 1), 1) }
    }

    @Test
    fun `month offsets and paged years stay within Int page indices`() {
        CalendarLimits.MAX_MONTH_OFFSET shouldBe Int.MAX_VALUE / 2
        CalendarLimits.clampMonthOffset(Int.MAX_VALUE) shouldBe CalendarLimits.MAX_MONTH_OFFSET
        CalendarLimits.clampMonthOffset(Int.MIN_VALUE) shouldBe -CalendarLimits.MAX_MONTH_OFFSET
        CalendarLimits.clampMonthOffset(-7) shouldBe -7
        val reach = CalendarLimits.MAX_MONTH_OFFSET / 12 - 1
        CalendarLimits.pagedYears(PersianCalendarSystem, today) shouldBe (1405 - reach)..(1405 + reach)
        val steppingReach = CalendarLimits.MAX_MONTH_OFFSET / 13 - 1
        CalendarLimits.pagedYears(SteppingPersian, today) shouldBe (1405 - steppingReach)..(1405 + steppingReach)
        val nearEnd = CalendarLimits.LAST_DAY - 400
        CalendarLimits.pagedYears(GregorianCalendarSystem, nearEnd).last shouldBe 999_999_999
        CalendarLimits.pagedYears(GregorianCalendarSystem, CalendarLimits.FIRST_DAY).first shouldBe -999_999_999
    }

    private fun persian(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.PERSIAN, year, month, day)

    private fun yearEnd(
        calendar: CalendarArithmetic,
        year: Int,
    ): Jdn {
        val lastMonth = calendar.monthsInYear(year)
        return calendar.toJdn(calendar.date(year, lastMonth, calendar.monthLength(year, lastMonth)))
    }
}
