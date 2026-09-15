/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1600: the watch month grid and the date converter. */
class WearMonthConverterTest {
    private val setup = WearFixtures.setup()
    private val lookup = WearFixtures.calculator.lookup(setup.islamicVariant)
    private val nowruz = WearFixtures.NOWRUZ_MORNING.toJdn(WearFixtures.TEHRAN_ZONE)

    @Test
    fun `Farvardin 1405 starts on its weekday, marks today and the Nowruz holidays`() {
        val month = WearMonthBuilder.build(setup, lookup, nowruz)
        val cells = month.weeks.flatten()

        month.title shouldBe "فروردین ۱۴۰۵"
        month.weekdayLabels shouldHaveSize 7
        month.weeks.all { it.size == 7 } shouldBe true
        cells.filterNotNull() shouldHaveSize 31
        cells.indexOfFirst { it != null } shouldBe WearMonthBuilder.columnOf(Weekday.SATURDAY, setup.weekStart)
        cells.filterNotNull().single { it.isToday }.day shouldBe 1
        cells.filterNotNull().first().isHoliday shouldBe true
        cells.filterNotNull().first().label shouldBe "۱"
    }

    @Test
    fun `other months follow and every week start places day 1 in its column`(): Unit =
        runBlocking {
            WearMonthBuilder.build(setup, lookup, nowruz, offsetMonths = 1).title shouldStartWith "اردیبهشت"
            WearMonthBuilder.build(setup, lookup, nowruz, offsetMonths = -1).title shouldBe "اسفند ۱۴۰۴"
            checkAll(50, Arb.enum<Weekday>(), Arb.int(-24..24)) { weekStart, offset ->
                val month = WearMonthBuilder.build(setup.copy(weekStart = weekStart), lookup, nowruz, offset)
                val first = month.weeks.flatten().indexOfFirst { it != null }
                (first in 0..6) shouldBe true
            }
        }

    @Test
    fun `converter steps wrap months and days and clamp to month lengths`() {
        val calendar = PersianCalendarSystem
        val lastOfShahrivar = calendar.date(1405, 6, 31)

        WearConverter.step(calendar, lastOfShahrivar, ConverterField.MONTH, 1) shouldBe calendar.date(1405, 7, 30)
        WearConverter.step(calendar, calendar.date(1405, 12, 29), ConverterField.DAY, 1) shouldBe
            calendar.date(1405, 12, 1)
        WearConverter.step(calendar, calendar.date(1405, 1, 1), ConverterField.MONTH, -1) shouldBe
            calendar.date(1405, 12, 1)
        WearConverter.step(calendar, calendar.date(1, 1, 1), ConverterField.YEAR, -1) shouldBe calendar.date(0, 1, 1)
        val years = CalendarLimits.years(calendar)
        WearConverter.step(calendar, calendar.date(years.last, 1, 1), ConverterField.YEAR, Int.MAX_VALUE) shouldBe
            calendar.date(years.last, 1, 1)
        WearConverter.step(calendar, calendar.date(years.first, 12, 1), ConverterField.YEAR, Int.MIN_VALUE) shouldBe
            calendar.date(years.first, 12, 1)
    }

    @Test
    fun `any step keeps a valid date and conversions keep the same day`(): Unit =
        runBlocking {
            val calendars = listOf(PersianCalendarSystem, GregorianCalendarSystem)
            checkAll(300, Arb.int(1300..1500), Arb.enum<ConverterField>(), Arb.int(-40..40)) { year, field, delta ->
                val calendar = calendars[Math.floorMod(year, 2)]
                val start = calendar.date(year, 1, 1)
                val moved = WearConverter.step(calendar, start, field, delta)
                calendar.isValid(moved.year, moved.month, moved.day) shouldBe true
                val other = calendars.first { it !== calendar }
                val converted = WearConverter.switchCalendar(calendar, other, moved)
                other.toJdn(converted) shouldBe calendar.toJdn(moved)
            }
        }

    @Test
    fun `Nowruz 1405 converts to 21 March 2026 and results skip the source calendar`() {
        WearConverter.switchCalendar(
            PersianCalendarSystem,
            GregorianCalendarSystem,
            CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1),
        ) shouldBe
            CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 21)
        val results = WearConverter.results(setup, PersianCalendarSystem, PersianCalendarSystem.date(1405, 1, 1))
        results.none { it.system == CalendarSystem.PERSIAN } shouldBe true
        results shouldHaveSize setup.calendars.size - 1
    }
}
