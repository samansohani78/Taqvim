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
import io.kotest.matchers.ints.shouldBeInRange
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

/** Golden tests for A-04's published years against ICU4J's Umm al-Qura calendar (Unicode License, test scope only). */
class UmmAlQuraCalendarTest {
    private val uaq = UmmAlQuraCalendar
    private val published = UmmAlQuraCalendar.PUBLISHED_FIRST_YEAR..UmmAlQuraCalendar.PUBLISHED_LAST_YEAR

    private fun regeneratedMasks(): String =
        published.joinToString(", ") { year ->
            val mask =
                (1..12).fold(0) { bits, month ->
                    if (UmmAlQuraIcu.length(year, month) == 30) bits or (1 shl (12 - month)) else bits
                }
            "0x%03X".format(mask)
        }

    @Test
    fun `every published month matches ICU4J`() {
        val mismatches =
            published.flatMap { year ->
                (1..12).mapNotNull { month ->
                    val ours = uaq.toJdn(hijri(year, month, 1)).value
                    val sameLength = uaq.monthLength(year, month) == UmmAlQuraIcu.length(year, month)
                    if (ours != UmmAlQuraIcu.start(year, month) || !sameLength) "$year-$month" else null
                }
            }

        withClue({ "ICU4J Umm al-Qura data changed; regenerated masks:\n${regeneratedMasks()}" }) {
            mismatches.shouldBeEmpty()
        }
    }

    @Test
    fun `agrees with ICU4J on 100 000 random days of the published years`() {
        val icu = UmmAlQuraIcu.calendar()
        val random = Random(SEED)
        val from = uaq.toJdn(hijri(published.first, 1, 1)).value
        val until = uaq.toJdn(hijri(published.last + 1, 1, 1)).value
        val mismatches =
            (1..SAMPLES).count {
                val jdn = random.nextLong(from, until)
                icu.clear()
                icu.set(Calendar.JULIAN_DAY, jdn.toInt())
                val expected =
                    hijri(icu.get(Calendar.EXTENDED_YEAR), icu.get(Calendar.MONTH) + 1, icu.get(Calendar.DAY_OF_MONTH))
                uaq.fromJdn(Jdn(jdn)) != expected || uaq.toJdn(expected) != Jdn(jdn)
            }

        mismatches shouldBe 0
    }

    @Test
    fun `computed years join the published calendar without gaps`() {
        uaq.isPublished(published.first - 1) shouldBe false
        uaq.isPublished(published.first) shouldBe true
        uaq.isPublished(published.last) shouldBe true
        uaq.isPublished(published.last + 1) shouldBe false
        listOf(published.first - 1 to 12, published.first to 1, published.last to 12, published.last + 1 to 1)
            .forEach { (year, month) -> uaq.monthLength(year, month) shouldBeInRange 29..30 }
        val firstDay = uaq.toJdn(hijri(published.first, 1, 1))
        uaq.fromJdn(firstDay) shouldBe hijri(published.first, 1, 1)
        uaq.fromJdn(Jdn(firstDay.value - 1)) shouldBe
            hijri(published.first - 1, 12, uaq.monthLength(published.first - 1, 12))
        uaq.fromJdn(uaq.toJdn(hijri(published.last + 1, 1, 1))) shouldBe hijri(published.last + 1, 1, 1)
    }

    @Test
    fun `leap years are the years longer than 354 days`() {
        (published.first - 20..published.last + 50).forEach { year ->
            val length = (1..12).sumOf { uaq.monthLength(year, it) }
            length shouldBeInRange 353..356
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
    fun `round-trips across and beyond the published years`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(2_300_000L..2_700_000L)) { value ->
                uaq.toJdn(uaq.fromJdn(Jdn(value))) shouldBe Jdn(value)
            }
        }

    private companion object {
        const val SAMPLES = 100_000
        const val SEED = 1_300L
    }
}

internal fun hijri(
    year: Int,
    month: Int,
    day: Int,
): CalendarDate = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

/** ICU4J's Umm al-Qura calendar as a black-box oracle (test scope only). */
internal object UmmAlQuraIcu {
    fun calendar(): IslamicCalendar =
        IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        }

    private fun atMonthStart(
        year: Int,
        month: Int,
    ): IslamicCalendar =
        calendar().apply {
            clear()
            set(Calendar.EXTENDED_YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

    /** JDN of the first day of [month] of [year]. */
    fun start(
        year: Int,
        month: Int,
    ): Long = atMonthStart(year, month).get(Calendar.JULIAN_DAY).toLong()

    /** Length of [month] of [year] in days. */
    fun length(
        year: Int,
        month: Int,
    ): Int = atMonthStart(year, month).getActualMaximum(Calendar.DAY_OF_MONTH)
}
