/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** Golden tests for A-04 against ICU4J's Umm al-Qura calendar (Unicode License, test scope only). */
class UmmAlQuraCalendarTest {
    private val uaq = UmmAlQuraCalendar
    private val civil = TabularIslamicCalendar.TYPE_II

    private fun hijri(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    private fun icu(): IslamicCalendar =
        IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        }

    private fun IslamicCalendar.atMonthStart(
        year: Int,
        month: Int,
    ): IslamicCalendar =
        apply {
            clear()
            set(Calendar.EXTENDED_YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

    private fun regeneratedMasks(icu: IslamicCalendar): String =
        (UmmAlQuraCalendar.FIRST_YEAR..UmmAlQuraCalendar.LAST_YEAR).joinToString(", ") { year ->
            val mask =
                (1..12).fold(0) { bits, month ->
                    val length = icu.atMonthStart(year, month).getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (length == 30) bits or (1 shl (12 - month)) else bits
                }
            "0x%03X".format(mask)
        }

    @Test
    fun `every tabulated month matches ICU4J`() {
        val icu = icu()
        val mismatches =
            (UmmAlQuraCalendar.FIRST_YEAR..UmmAlQuraCalendar.LAST_YEAR).flatMap { year ->
                (1..12).mapNotNull { month ->
                    val start = icu.atMonthStart(year, month).get(Calendar.JULIAN_DAY).toLong()
                    val length = icu.atMonthStart(year, month).getActualMaximum(Calendar.DAY_OF_MONTH)
                    val ours = uaq.toJdn(hijri(year, month, 1)).value
                    if (ours != start || uaq.monthLength(year, month) != length) "$year-$month" else null
                }
            }

        withClue({ "ICU4J Umm al-Qura data changed; regenerated masks:\n${regeneratedMasks(icu)}" }) {
            mismatches.shouldBeEmpty()
        }
    }

    @Test
    fun `agrees with ICU4J on 100 000 random days in and around the table`() {
        val icu = icu()
        val random = Random(SEED)
        val from = civil.toJdn(hijri(1200, 1, 1)).value
        val until = civil.toJdn(hijri(1700, 1, 1)).value
        val mismatches =
            (1..SAMPLES).count {
                val jdn = random.nextLong(from, until)
                icu.clear()
                icu.set(Calendar.JULIAN_DAY, jdn.toInt())
                val expected = icu.currentHijriDate()
                uaq.fromJdn(Jdn(jdn)) != expected || uaq.toJdn(expected) != Jdn(jdn)
            }

        mismatches shouldBe 0
    }

    @Test
    fun `table boundaries join the civil calendar without gaps`() {
        uaq.fromJdn(Jdn(UmmAlQuraCalendar.TABLE_START_JDN)) shouldBe hijri(1300, 1, 1)
        val dayBeforeTable = Jdn(UmmAlQuraCalendar.TABLE_START_JDN - 1)
        uaq.fromJdn(dayBeforeTable) shouldBe civil.fromJdn(dayBeforeTable)
        uaq.fromJdn(Jdn(UmmAlQuraCalendar.TABLE_START_JDN - 1)).year shouldBe 1299
        uaq.fromJdn(Jdn(UmmAlQuraCalendar.TABLE_END_JDN - 1)).year shouldBe 1600
        uaq.fromJdn(Jdn(UmmAlQuraCalendar.TABLE_END_JDN)) shouldBe hijri(1601, 1, 1)
        civil.toJdn(hijri(1300, 1, 1)) shouldBe Jdn(UmmAlQuraCalendar.TABLE_START_JDN)
        civil.toJdn(hijri(1601, 1, 1)) shouldBe Jdn(UmmAlQuraCalendar.TABLE_END_JDN)
    }

    @Test
    fun `outside the table the civil calendar applies`() {
        uaq.isTabulated(1299) shouldBe false
        uaq.isTabulated(1447) shouldBe true
        uaq.toJdn(hijri(1000, 5, 10)) shouldBe civil.toJdn(hijri(1000, 5, 10))
        uaq.isLeapYear(1002) shouldBe civil.isLeapYear(1002)
        uaq.monthLength(1700, 12) shouldBe civil.monthLength(1700, 12)
        uaq.fromJdn(Jdn(0)) shouldBe civil.fromJdn(Jdn(0))
    }

    @Test
    fun `leap years are the 355-day years of the table`() {
        (UmmAlQuraCalendar.FIRST_YEAR..UmmAlQuraCalendar.LAST_YEAR).forEach { year ->
            val length = (1..12).sumOf { uaq.monthLength(year, it) }
            uaq.isLeapYear(year) shouldBe (length > 354)
        }
        uaq.monthsInYear(1447) shouldBe 12
    }

    @Test
    fun `invalid dates and other systems are rejected`() {
        val shortMonth = (1..12).first { uaq.monthLength(1447, it) == 29 }

        shouldThrow<IllegalArgumentException> { uaq.toJdn(hijri(1447, shortMonth, 30)) }
        shouldThrow<IllegalArgumentException> { uaq.monthLength(1447, 13) }
        shouldThrow<IllegalArgumentException> { uaq.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2026, 1, 1)) }
    }

    @Test
    fun `round-trips across and beyond the table`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(1_900_000L..2_600_000L)) { value ->
                uaq.toJdn(uaq.fromJdn(Jdn(value))) shouldBe Jdn(value)
            }
        }

    private companion object {
        const val SAMPLES = 100_000
        const val SEED = 1_300L
    }
}
