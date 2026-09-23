/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-109 Julian calendar and computus, and movable feasts in the civil calendar of every year. */
class JulianComputusTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private fun julian(
        year: Long,
        month: Int,
        day: Int,
    ) = JulianDate(year, month, day)

    private fun gregorianJdn(
        year: Int,
        month: Int,
        day: Int,
    ) = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day))

    @Test
    fun `Julian Easter matches the examples Meeus lists`() {
        // Meeus, Astronomical Algorithms (2nd ed.), ch. 8: Julian-calendar examples.
        JulianComputus.easter(179) shouldBe julian(179, 4, 12)
        JulianComputus.easter(711) shouldBe julian(711, 4, 12)
        JulianComputus.easter(1_243) shouldBe julian(1_243, 4, 12)
        JulianComputus.easter(1_582) shouldBe julian(1_582, 4, 15)
    }

    @Test
    fun `Julian days agree with the calendar epochs the repository already uses`() {
        JulianCalendar.toJdn(julian(0, 1, 1)).value shouldBe JulianCalendar.JDN_OF_YEAR_ZERO
        JulianCalendar.toJdn(julian(622, 7, 16)).value shouldBe TabularIslamicCalendar.EPOCH_JDN
        JulianCalendar.toJdn(julian(-3_760, 10, 7)).value shouldBe HebrewCalendar.EPOCH_JDN
        JulianCalendar.toJdn(julian(1_582, 10, 4)) + 1 shouldBe gregorianJdn(1_582, 10, 15)
        JulianCalendar.toJdn(julian(1_999, 12, 19)) shouldBe gregorianJdn(2_000, 1, 1)
        JulianCalendar.toJdn(julian(JulianCalendar.MIN_YEAR, 1, 1)).value shouldBe JulianCalendar.FIRST_JDN
        JulianCalendar.toJdn(julian(JulianCalendar.MAX_YEAR, 12, 31)).value shouldBe JulianCalendar.LAST_JDN
    }

    @Test
    fun `Julian days round trip across the whole range`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(JulianCalendar.FIRST_JDN..JulianCalendar.LAST_JDN)) { day ->
                JulianCalendar.toJdn(JulianCalendar.fromJdn(Jdn(day))).value shouldBe day
            }
            (-3_000L..3_000L).forEach { day ->
                val jdn = Jdn(JulianCalendar.JDN_OF_YEAR_ZERO + day)
                JulianCalendar.toJdn(JulianCalendar.fromJdn(jdn)) shouldBe jdn
            }
            listOf(JulianCalendar.FIRST_JDN, JulianCalendar.LAST_JDN).forEach { day ->
                JulianCalendar.toJdn(JulianCalendar.fromJdn(Jdn(day))).value shouldBe day
            }
        }

    @Test
    fun `leap years and invalid days follow the Julian rule`() {
        JulianCalendar.isLeapYear(1_500) shouldBe true
        JulianCalendar.isLeapYear(-4) shouldBe true
        JulianCalendar.isLeapYear(-1) shouldBe false
        JulianCalendar.monthLength(1_580, 2) shouldBe 29
        JulianCalendar.monthLength(1_581, 2) shouldBe 28
        shouldThrow<IllegalArgumentException> { JulianCalendar.toJdn(julian(1_581, 2, 29)) }
        shouldThrow<IllegalArgumentException> { JulianCalendar.monthLength(1_581, 13) }
        shouldThrow<IllegalArgumentException> { julian(1_581, 0, 1) }
        shouldThrow<IllegalArgumentException> { julian(1_581, 1, 0) }
        shouldThrow<IllegalArgumentException> { JulianCalendar.toJdn(julian(JulianCalendar.MAX_YEAR + 1, 1, 1)) }
        shouldThrow<IllegalArgumentException> { JulianCalendar.fromJdn(Jdn(JulianCalendar.LAST_JDN + 1)) }
        shouldThrow<IllegalArgumentException> { JulianCalendar.fromJdn(Jdn(Long.MIN_VALUE)) }
        shouldThrow<IllegalArgumentException> { JulianComputus.easter(JulianCalendar.MIN_YEAR - 1) }
    }

    @Test
    fun `Julian Easter is a Sunday from 22 March to 25 April in every year`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(-TEN_MILLION..TEN_MILLION)) { assertJulianEaster(it) }
            checkAll(propertyConfig, Arb.long(JulianCalendar.MIN_YEAR..JulianCalendar.MAX_YEAR)) {
                assertJulianEaster(it)
            }
            assertJulianEaster(JulianCalendar.MIN_YEAR)
            assertJulianEaster(JulianCalendar.MAX_YEAR)
            assertJulianEaster(-TEN_MILLION)
            assertJulianEaster(TEN_MILLION)
        }

    @Test
    fun `Julian Easter dates repeat every 532 years`(): Unit =
        runBlocking {
            val lastStart = JulianCalendar.MAX_YEAR - JulianComputus.CYCLE_YEARS
            checkAll(propertyConfig, Arb.long(JulianCalendar.MIN_YEAR..lastStart)) { year ->
                val later = JulianComputus.easter(year + JulianComputus.CYCLE_YEARS)
                (later.month to later.day) shouldBe JulianComputus.easter(year).let { it.month to it.day }
            }
            listOf(JulianCalendar.MIN_YEAR, lastStart).forEach { year ->
                val later = JulianComputus.easter(year + JulianComputus.CYCLE_YEARS)
                (later.month to later.day) shouldBe JulianComputus.easter(year).let { it.month to it.day }
            }
        }

    @Test
    fun `civil feasts switch from the Julian to the Gregorian computus at the reform`() {
        val feasts1582 = ChristianMovableFeasts.civilForYear(1_582)
        feasts1582.getValue(MovableFeast.EASTER) shouldBe JulianCalendar.toJdn(julian(1_582, 4, 15))
        feasts1582.getValue(MovableFeast.FIRST_SUNDAY_OF_ADVENT) shouldBe gregorianJdn(1_582, 11, 28)
        val feasts1583 = ChristianMovableFeasts.forYear(GregorianComputus.FIRST_YEAR)
        ChristianMovableFeasts.civilForYear(1_583) shouldBe
            feasts1583.mapValues { GregorianCalendarSystem.toJdn(it.value) }
        ChristianMovableFeasts.civilForYear(1_581).getValue(MovableFeast.FIRST_SUNDAY_OF_ADVENT) shouldBe
            JulianCalendar.toJdn(julian(1_581, 12, 3))
        ChristianMovableFeasts.julianForYear(1_243).getValue(MovableFeast.EASTER) shouldBe julian(1_243, 4, 12)
        ChristianMovableFeasts.julianForYear(1_243).getValue(MovableFeast.PENTECOST) shouldBe julian(1_243, 5, 31)
        shouldThrow<IllegalArgumentException> { ChristianMovableFeasts.civilForYear(GregorianComputus.LAST_YEAR + 1L) }
        shouldThrow<IllegalArgumentException> { ChristianMovableFeasts.civilForYear(JulianCalendar.MIN_YEAR - 1) }
    }

    @Test
    fun `civil feasts keep their weekdays and offsets in every year`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(-TEN_MILLION..GregorianComputus.LAST_YEAR.toLong())) {
                assertCivilFeasts(it)
            }
            (1_570L..1_600L).forEach(::assertCivilFeasts)
            assertCivilFeasts(JulianCalendar.MIN_YEAR)
            assertCivilFeasts(-TEN_MILLION)
            assertCivilFeasts(GregorianComputus.LAST_YEAR.toLong())
        }

    @Test
    fun `Orthodox Easter is the Julian computus on the Gregorian calendar`(): Unit =
        runBlocking {
            val years = Arb.int(GregorianComputus.FIRST_YEAR..JulianComputus.ORTHODOX_LAST_YEAR)
            checkAll(propertyConfig, years) { year ->
                val orthodox = GregorianCalendarSystem.toJdn(JulianComputus.orthodoxEaster(year))
                orthodox shouldBe JulianComputus.easterJdn(year.toLong())
                orthodox.weekday() shouldBe Weekday.SUNDAY
                val western = GregorianCalendarSystem.toJdn(GregorianComputus.easter(year))
                (orthodox - western) % DAYS_PER_WEEK shouldBe 0L
            }
            listOf(GregorianComputus.FIRST_YEAR, JulianComputus.ORTHODOX_LAST_YEAR).forEach { year ->
                val orthodox = GregorianCalendarSystem.toJdn(JulianComputus.orthodoxEaster(year))
                orthodox shouldBe JulianComputus.easterJdn(year.toLong())
                orthodox.weekday() shouldBe Weekday.SUNDAY
            }
            JulianComputus.orthodoxEaster(2_024) shouldBe CalendarDate(CalendarSystem.GREGORIAN, 2_024, 5, 5)
            JulianComputus.orthodoxEaster(JulianComputus.ORTHODOX_LAST_YEAR).year shouldBe Int.MAX_VALUE
            shouldThrow<IllegalArgumentException> { JulianComputus.orthodoxEaster(1_582) }
            shouldThrow<IllegalArgumentException> {
                JulianComputus.orthodoxEaster(
                    JulianComputus.ORTHODOX_LAST_YEAR + 1,
                )
            }
        }

    private fun assertJulianEaster(year: Long) {
        val easter = JulianComputus.easter(year)
        JulianCalendar.toJdn(easter).weekday() shouldBe Weekday.SUNDAY
        easter.year shouldBe year
        (easter.month * MONTH_DAY_SCALE + easter.day) shouldBeGreaterThanOrEqualTo EARLIEST_EASTER
        (easter.month * MONTH_DAY_SCALE + easter.day) shouldBeLessThanOrEqualTo LATEST_EASTER
    }

    private fun assertCivilFeasts(year: Long) {
        val feasts = ChristianMovableFeasts.civilForYear(year)
        val easter = feasts.getValue(MovableFeast.EASTER)
        easter.weekday() shouldBe Weekday.SUNDAY
        feasts.getValue(MovableFeast.FIRST_SUNDAY_OF_ADVENT).weekday() shouldBe Weekday.SUNDAY
        MovableFeast.entries.forEach { feast ->
            feast.daysFromEaster?.let { offset -> feasts.getValue(feast) - easter shouldBe offset.toLong() }
        }
        feasts.values.toList() shouldBe feasts.values.sorted()
    }

    private companion object {
        const val TEN_MILLION = 10_000_000L
        const val MONTH_DAY_SCALE = 100
        const val EARLIEST_EASTER = 322
        const val LATEST_EASTER = 425
        const val DAYS_PER_WEEK = 7L
    }
}
