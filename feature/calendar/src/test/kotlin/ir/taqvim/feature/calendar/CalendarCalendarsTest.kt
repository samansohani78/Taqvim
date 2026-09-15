/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-800 helpers: available calendars, month offsets, "today" ticks and the search use case. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarCalendarsTest {
    private val today = gregorian(2026, 3, 18)

    @Test
    fun `unavailable and repeated calendars are skipped, Gregorian when none is left`() {
        val repeated = listOf(CalendarSystem.NEPALI, CalendarSystem.PERSIAN, CalendarSystem.PERSIAN)
        val calendars = CalendarCalendars(PERSIAN_FIRST.copy(calendars = repeated))
        calendars.systems shouldBe listOf(CalendarSystem.PERSIAN)
        calendars.datesOf(today) shouldBe listOf(CalendarDate(CalendarSystem.PERSIAN, 1404, 12, 27))

        CalendarCalendars(PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.NEPALI))).systems shouldBe
            listOf(CalendarSystem.GREGORIAN)
    }

    @Test
    fun `arithmetic per calendar follows the Islamic variant`() {
        CalendarCalendars.arithmeticFor(CalendarSystem.PERSIAN, IslamicVariant.IRAN_OFFICIAL) shouldBeSameInstanceAs
            PersianCalendarSystem
        CalendarCalendars.arithmeticFor(CalendarSystem.GREGORIAN, IslamicVariant.IRAN_OFFICIAL) shouldBeSameInstanceAs
            GregorianCalendarSystem
        CalendarCalendars.arithmeticFor(CalendarSystem.ISLAMIC, IslamicVariant.UMM_AL_QURA) shouldBeSameInstanceAs
            UmmAlQuraCalendar
        CalendarCalendars.arithmeticFor(CalendarSystem.NEPALI, IslamicVariant.IRAN_OFFICIAL).shouldBeNull()
    }

    @Test
    fun `month offsets count whole months of the primary calendar across years`() {
        val persian = CalendarCalendars(PERSIAN_FIRST)
        persian.monthOffset(today, gregorian(2025, 3, 21)) shouldBe -11
        persian.monthStartAt(today, -11) shouldBe gregorian(2025, 3, 21)
        persian.monthStartAt(today, 1) shouldBe gregorian(2026, 3, 21)

        val gregorianOnly = CalendarCalendars(PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.GREGORIAN)))
        gregorianOnly.monthOffset(today, gregorian(2024, 2, 29)) shouldBe -25
        gregorianOnly.monthStartAt(today, -25) shouldBe gregorian(2024, 2, 1)
        gregorianOnly.monthStart(today) shouldBe CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 1)
    }

    @Test
    fun `the pager's farthest months and the go-to-date years are reached directly`() {
        val persian = CalendarCalendars(PERSIAN_FIRST)
        val far = CalendarLimits.MAX_MONTH_OFFSET
        val last = persian.monthStartAt(today, far)
        val first = persian.monthStartAt(today, -far)
        persian.monthOffset(today, last) shouldBe far
        persian.monthOffset(today, first) shouldBe -far
        persian.monthStartAt(today, MonthPages.offsetOf(MonthPages.COUNT - 1)) shouldBe last
        val years = persian.pagedYears(today)
        years shouldBe CalendarLimits.pagedYears(PersianCalendarSystem, today)
        PersianCalendarSystem.fromJdn(persian.monthStartAt(today, MonthPages.offsetOf(0))).year shouldBe
            years.first - 1
        MonthPages.pageOf(0) shouldBe MonthPages.TODAY_PAGE
        MonthPages.pageOf(Int.MAX_VALUE) shouldBe MonthPages.COUNT - 1
        MonthPages.pageOf(Int.MIN_VALUE) shouldBe 0
        MonthPages.offsetOf(-5) shouldBe -far
        MonthPages.offsetOf(Int.MAX_VALUE) shouldBe far
        MonthPages.COUNT shouldBe Int.MAX_VALUE
        listOf(-far, -1, 0, 1, 12_000, far).forEach { MonthPages.offsetOf(MonthPages.pageOf(it)) shouldBe it }
    }

    @Test
    fun `today is read on every tick and emitted only when the day changes`(): Unit =
        runTest {
            var current = today
            TickingTodaySource({ current }, 1.minutes).today().test {
                awaitItem() shouldBe today
                advanceTimeBy(5.minutes)
                runCurrent()
                expectNoEvents()

                current = today + 1
                advanceTimeBy(1.minutes)
                runCurrent()
                awaitItem() shouldBe today + 1
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the tick interval must be positive`() {
        shouldThrow<IllegalArgumentException> { TickingTodaySource({ today }, Duration.ZERO) }
    }

    @Test
    fun `search trims queries, passes the limit and skips blank ones`(): Unit =
        runTest {
            val source = FakeSearchSource(mapOf("nowruz" to emptyList()))
            val search = SearchEventsUseCase(source)

            search("  ").shouldBeEmpty()
            source.queries.shouldBeEmpty()
            search("  nowruz ").shouldBeEmpty()
            source.queries shouldBe listOf("nowruz" to SearchEventsUseCase.LIMIT)
        }
}
