/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.DatePeriod
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.calendar.plusDays
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1212 (F-09) countdown arithmetic across calendars, leap days and 30 Esfand. */
class WidgetCountdownMathTest {
    private val calendars: List<CalendarArithmetic> =
        listOf(PersianCalendarSystem, GregorianCalendarSystem, TabularIslamicCalendar.TYPE_II)

    /** Days from 1900 to 2100 (Gregorian). */
    private val days: Arb<Jdn> =
        Arb
            .long(
                LocalDate(1900, 1, 1).toJdn().value..LocalDate(2100, 12, 31).toJdn().value,
            ).map {
                Jdn(it)
            }

    private fun countdown(
        calendar: CalendarArithmetic,
        origin: Jdn,
        mode: CountdownMode = CountdownMode.UNTIL,
        repeats: Boolean = false,
        start: Jdn = origin,
    ): WidgetCountdown {
        val date = calendar.fromJdn(origin)
        return WidgetCountdown(calendar.system, date.year, date.month, date.day, start.value, mode, repeats)
    }

    private fun inRange(progress: Float) = (progress in 0f..1f).shouldBeTrue()

    @Test
    fun `yearly countdowns target the next anniversary with the day clamped to its month`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.element(calendars), days, days) { calendar, originJdn, today ->
                val origin = calendar.fromJdn(originJdn)
                val result =
                    WidgetCountdownMath.evaluate(
                        countdown(calendar, originJdn, repeats = true),
                        calendar,
                        today,
                    )
                val target = calendar.fromJdn(result.target)

                (result.target >= today).shouldBeTrue()
                result.days shouldBe result.target - today
                target.month shouldBe origin.month
                target.day shouldBe minOf(origin.day, calendar.monthLength(target.year, target.month))
                val previous = WidgetCountdownMath.anniversary(origin, target.year - 1, calendar)
                (calendar.toJdn(previous) < today).shouldBeTrue()
                result.status shouldBe if (result.days == 0L) CountdownStatus.TODAY else CountdownStatus.UPCOMING
                inRange(result.progress)
            }
        }

    @Test
    fun `time since a date adds up to today in the date's calendar`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.element(calendars), days, days) { calendar, first, second ->
                val (originJdn, today) = if (first <= second) first to second else second to first
                val origin = calendar.fromJdn(originJdn)
                val result =
                    WidgetCountdownMath.evaluate(countdown(calendar, originJdn, CountdownMode.SINCE), calendar, today)
                val period = requireNotNull(result.period)

                val anchor = calendar.addMonths(origin, period.years * 12 + period.months)
                calendar.plusDays(anchor, period.days.toLong()) shouldBe calendar.fromJdn(today)
                (period.years >= 0 && period.months in 0..11 && period.days >= 0).shouldBeTrue()
                result.days shouldBe today - originJdn
                result.target shouldBe originJdn
                result.status shouldBe if (today == originJdn) CountdownStatus.TODAY else CountdownStatus.ELAPSED
                inRange(result.progress)
            }
        }

    @Test
    fun `one-off countdowns count to their date and time since a future date has not started`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.element(calendars), days, days) { calendar, originJdn, today ->
                val result =
                    WidgetCountdownMath.evaluate(countdown(calendar, originJdn, start = today - 30), calendar, today)

                result.target shouldBe originJdn
                result.days shouldBe abs(originJdn - today)
                result.status shouldBe
                    when {
                        originJdn > today -> CountdownStatus.UPCOMING
                        originJdn == today -> CountdownStatus.TODAY
                        else -> CountdownStatus.PASSED
                    }
                if (originJdn <= today) result.progress shouldBe 1f else inRange(result.progress)
                if (originJdn > today) {
                    val since =
                        WidgetCountdownMath.evaluate(
                            countdown(calendar, originJdn, CountdownMode.SINCE),
                            calendar,
                            today,
                        )
                    since.status shouldBe CountdownStatus.NOT_YET
                    since.days shouldBe originJdn - today
                    since.period.shouldBeNull()
                }
            }
        }

    @Test
    fun `30 Esfand falls on 29 Esfand in common years and returns in leap years`() {
        val persian = PersianCalendarSystem
        val leap = (1395..1430).first { persian.isLeapYear(it) && !persian.isLeapYear(it + 1) }
        val esfand30 = WidgetCountdown(CalendarSystem.PERSIAN, leap, 12, 30, startJdn = 0, repeatsYearly = true)

        fun day(
            year: Int,
            month: Int,
            day: Int,
        ) = persian.toJdn(persian.date(year, month, day))

        WidgetCountdownMath.evaluate(esfand30, persian, day(leap + 1, 1, 1)).target shouldBe day(leap + 1, 12, 29)
        val nextLeap = ((leap + 1)..(leap + 8)).first { persian.isLeapYear(it) }
        WidgetCountdownMath.evaluate(esfand30, persian, day(nextLeap, 12, 1)).target shouldBe day(nextLeap, 12, 30)
        WidgetCountdownMath.evaluate(esfand30, persian, day(leap, 12, 30)).status shouldBe CountdownStatus.TODAY

        val age = esfand30.copy(mode = CountdownMode.SINCE, repeatsYearly = false)
        WidgetCountdownMath.evaluate(age, persian, day(leap + 1, 12, 29)).period shouldBe DatePeriod(1, 0, 0)
        WidgetCountdownMath.evaluate(age, persian, day(leap + 2, 1, 1)).period shouldBe DatePeriod(1, 0, 1)
        // A 30 Esfand stored for a common year is read as 29 Esfand.
        WidgetCountdownMath.origin(esfand30.copy(year = leap + 1), persian) shouldBe persian.date(leap + 1, 12, 29)
        WidgetCountdownMath.origin(esfand30.copy(month = 13, day = 40), persian) shouldBe persian.date(leap, 12, 30)
    }

    @Test
    fun `29 February counts to 28 February in common years`() {
        val gregorian = GregorianCalendarSystem
        val leapDay = WidgetCountdown(CalendarSystem.GREGORIAN, 2024, 2, 29, startJdn = 0, repeatsYearly = true)

        fun day(
            year: Int,
            month: Int,
            day: Int,
        ) = LocalDate(year, month, day).toJdn()

        WidgetCountdownMath.evaluate(leapDay, gregorian, day(2025, 3, 1)).target shouldBe day(2026, 2, 28)
        WidgetCountdownMath.evaluate(leapDay, gregorian, day(2027, 6, 1)).target shouldBe day(2028, 2, 29)
        WidgetCountdownMath.evaluate(leapDay, gregorian, day(2028, 2, 29)).status shouldBe CountdownStatus.TODAY

        val age = leapDay.copy(mode = CountdownMode.SINCE, repeatsYearly = false)
        WidgetCountdownMath.evaluate(age, gregorian, day(2025, 2, 28)).period shouldBe DatePeriod(1, 0, 0)
        WidgetCountdownMath.evaluate(age, gregorian, day(2025, 3, 1)).period shouldBe DatePeriod(1, 0, 1)
        WidgetCountdownMath.evaluate(age, gregorian, day(2024, 2, 28)).status shouldBe CountdownStatus.NOT_YET
    }

    @Test
    fun `one-off progress runs from the day the date was chosen`() {
        val gregorian = GregorianCalendarSystem
        val start = LocalDate(2026, 9, 1).toJdn()
        val trip = WidgetCountdown(CalendarSystem.GREGORIAN, 2026, 9, 11, startJdn = start.value)

        val halfway = WidgetCountdownMath.evaluate(trip, gregorian, LocalDate(2026, 9, 6).toJdn())
        halfway.progress shouldBe 0.5f
        halfway.days shouldBe 5
        WidgetCountdownMath.evaluate(trip, gregorian, LocalDate(2026, 9, 11).toJdn()).progress shouldBe 1f
        val passed = WidgetCountdownMath.evaluate(trip, gregorian, LocalDate(2026, 9, 20).toJdn())
        passed.status shouldBe CountdownStatus.PASSED
        passed.days shouldBe 9
        // Chosen on the day itself: nothing to measure, so the ring is full.
        WidgetCountdownMath
            .evaluate(trip.copy(startJdn = LocalDate(2026, 9, 12).toJdn().value), gregorian, start)
            .progress shouldBe 1f
    }
}
