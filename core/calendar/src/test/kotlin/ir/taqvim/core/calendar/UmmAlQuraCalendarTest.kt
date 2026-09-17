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

/** Golden tests for A-04's bundled years against ICU4J's Umm al-Qura calendar (Unicode License, test scope only). */
class UmmAlQuraCalendarTest {
    private val uaq = UmmAlQuraCalendar
    private val bundled = UmmAlQuraCalendar.BUNDLED_FIRST_YEAR..UmmAlQuraCalendar.BUNDLED_LAST_YEAR

    private fun regeneratedMasks(): String =
        bundled.joinToString(", ") { year ->
            val mask =
                (1..12).fold(0) { bits, month ->
                    if (UmmAlQuraIcu.length(year, month) == 30) bits or (1 shl (12 - month)) else bits
                }
            "0x%03X".format(mask)
        }

    @Test
    fun `every bundled month matches ICU4J`() {
        val mismatches =
            bundled.flatMap { year ->
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
    fun `agrees with ICU4J on 100 000 random days of the bundled years`() {
        val icu = UmmAlQuraIcu.calendar()
        val random = Random(SEED)
        val from = uaq.toJdn(hijri(bundled.first, 1, 1)).value
        val until = uaq.toJdn(hijri(bundled.last + 1, 1, 1)).value
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
    fun `computed years join the bundled calendar without gaps`() {
        uaq.isBundled(bundled.first - 1) shouldBe false
        uaq.isBundled(bundled.first) shouldBe true
        uaq.isBundled(bundled.last) shouldBe true
        uaq.isBundled(bundled.last + 1) shouldBe false
        listOf(bundled.first - 1 to 12, bundled.first to 1, bundled.last to 12, bundled.last + 1 to 1)
            .forEach { (year, month) -> uaq.monthLength(year, month) shouldBeInRange 29..30 }
        val firstDay = uaq.toJdn(hijri(bundled.first, 1, 1))
        uaq.fromJdn(firstDay) shouldBe hijri(bundled.first, 1, 1)
        uaq.fromJdn(Jdn(firstDay.value - 1)) shouldBe
            hijri(bundled.first - 1, 12, uaq.monthLength(bundled.first - 1, 12))
        uaq.fromJdn(uaq.toJdn(hijri(bundled.last + 1, 1, 1))) shouldBe hijri(bundled.last + 1, 1, 1)
    }

    @Test
    fun `only the years no rule reproduces are bundled`() {
        UMM_AL_QURA_MONTH_MASKS.size shouldBe bundled.count()
        bundled shouldBe 1300..1419
    }

    @Test
    fun `the published months of 1420 to 1450 are ICU4J's data`() {
        PUBLISHED_1420_1450_MASKS.size shouldBe 31
        PUBLISHED_1420_1450_MASKS.forEachIndexed { offset, mask ->
            (1..12).forEach { month ->
                val length = if (mask shr (12 - month) and 1 == 1) 30 else 29
                withClue(1420 + offset to month) { UmmAlQuraIcu.length(1420 + offset, month) shouldBe length }
            }
        }
    }

    @Test
    fun `computed months of 1420 to 1450 match the published calendar but two marginal ones`() {
        val differing =
            (1420..1450).flatMap { year ->
                (1..12).mapNotNull { month ->
                    val published = publishedStart(year, month)
                    val ours = uaq.toJdn(hijri(year, month, 1)).value
                    if (ours != published) Triple(year, month, ours - published) else null
                }
            }

        // ADR-0028: Jumada II 1427 (conjunction 24 s before sunset) and Jumada II 1446 (moonset 4 s before sunset).
        differing shouldBe EXPECTED_MARGINAL_MONTHS
    }

    /** First day of [month] of [year] in the published calendar, from the golden masks of 1420–1450. */
    private fun publishedStart(
        year: Int,
        month: Int,
    ): Long {
        val lengths =
            PUBLISHED_1420_1450_MASKS.flatMap { mask ->
                (1..12).map { if (mask shr (12 - it) and 1 == 1) 30 else 29 }
            }
        val before = (year - 1420) * 12 + month - 1
        return uaq.toJdn(hijri(1420, 1, 1)).value + lengths.take(before).sum()
    }

    @Test
    fun `leap years are the years longer than 354 days`() {
        (bundled.first - 20..bundled.last + 80).forEach { year ->
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
    fun `round-trips across and beyond the bundled years`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(2_300_000L..2_700_000L)) { value ->
                uaq.toJdn(uaq.fromJdn(Jdn(value))) shouldBe Jdn(value)
            }
        }

    private companion object {
        const val SAMPLES = 100_000
        const val SEED = 1_300L

        /**
         * Month starts of 1420–1450 where the computed calendar differs from the published one, with the difference in
         * days (computed − published): 1 Jumada II 1427 is a day earlier and 1 Jumada II 1446 a day later; the months
         * after them start on the published days again.
         */
        val EXPECTED_MARGINAL_MONTHS: List<Triple<Int, Int, Long>> = listOf(Triple(1427, 6, -1L), Triple(1446, 6, 1L))

        /**
         * Golden oracle: the published Umm al-Qura month lengths of AH 1420–1450 (Saudi Ministry of Finance comparison
         * calendar 1420–1450 AH, as carried by ICU4J 78.3; one 12-bit mask per year, bit 11 = month 1, set bit = 30
         * days). Test data only; the app computes these years (ADR-0028 addendum 2026-09-17).
         */
        val PUBLISHED_1420_1450_MASKS: List<Int> =
            listOf(
                0x4BD,
                0x23D,
                0x91D,
                0xA95,
                0xB4A,
                0xB5A,
                0x56D,
                0x2B6,
                0x93B,
                0x49B,
                0x655,
                0x6A9,
                0x754,
                0xB6A,
                0x56C,
                0xAAD,
                0x555,
                0xB29,
                0xB92,
                0xBA9,
                0x5D4,
                0xADA,
                0x55A,
                0xAAB,
                0x595,
                0x749,
                0x764,
                0xBAA,
                0x5B5,
                0x2B6,
                0xA56,
            )
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
