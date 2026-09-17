/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn
import kotlin.math.floor

/** The moment of a civil day at which its tithi is read (ADR-0038). */
public enum class TithiObservance {
    /** The tithi current at sunrise in Kathmandu names the day (udaya tithi, the usual rule). */
    SUNRISE,

    /** The tithi current at sunset (pradosha), e.g. Laxmi Puja on the new moon. */
    SUNSET,

    /** The tithi current at the midnight that ends the day (nishitha), e.g. Maha Shivaratri. */
    MIDNIGHT,
}

/**
 * Days of lunar festivals in Bikram Sambat years, from the Surya Siddhanta Sun and Moon (ADR-0038).
 *
 * A tithi is a twelfth of a degree-360 lap of the Moon's elongation: tithi n (1‥30) runs while the elongation is in
 * [12(n − 1)°, 12n°); 1‥15 are the bright half (shukla paksha) and 16‥30 the dark half, 30 being the new moon. Lunar
 * months are amanta: they run from new moon to new moon and are named by the solar month (1 = Baisakh) in which their
 * first new moon falls; when a solar month holds two new moons the first month is intercalary (adhika) and the second
 * carries the name, and when it holds none (kshaya) that month has no festivals. Purnimanta names of the dark half map
 * to the next amanta month number (Bhadra Krishna Ashtami of the panchang is amanta Shrawan, month 4, tithi 23).
 *
 * A festival day is the first civil day whose observed tithi is the festival's; if the tithi is skipped (it begins
 * and ends between two observation moments), it is the last day before the tithi, the day on which it began.
 */
public object NepaliLunarDays {
    /** Tithis in a lunar month. */
    public const val TITHIS_IN_MONTH: Int = 30

    private const val MONTHS = 12

    /**
     * Every day of Bikram Sambat [year] of the festival that falls on [tithi] of lunar month [month], observed at
     * [observance]. With [endTithi] the festival lasts until the day of the first [endTithi] after [tithi] (observed at
     * sunrise, in the next lunar month when [endTithi] ≤ [tithi]) plus [endOffsetDays]. A festival that begins in the
     * previous year contributes only its days in [year]; so does one that ends in the next year.
     */
    public fun days(
        year: Int,
        month: Int,
        tithi: Int,
        observance: TithiObservance = TithiObservance.SUNRISE,
        endTithi: Int? = null,
        endOffsetDays: Int = 0,
    ): List<Jdn> {
        require(month in 1..MONTHS) { "lunar month must be in 1..12 (was $month)" }
        require(tithi in 1..TITHIS_IN_MONTH) { "tithi must be in 1..30 (was $tithi)" }
        require(endTithi == null || endTithi in 1..TITHIS_IN_MONTH) { "endTithi must be in 1..30 (was $endTithi)" }
        require(endOffsetDays >= 0) { "endOffsetDays must be ≥ 0 (was $endOffsetDays)" }
        val first = NepaliMonthStarts.startJdn(year.toLong(), 1)
        val next = NepaliMonthStarts.startJdn(year + 1L, 1)
        return listOf(year - 1L, year.toLong())
            .mapNotNull { LunarMonth.of(it, month) }
            .flatMap { lunar ->
                val start = lunar.dayOf(tithi, observance)
                val last =
                    endTithi?.let {
                        lunar.dayOf(if (it > tithi) it else it + TITHIS_IN_MONTH, TithiObservance.SUNRISE)
                    } ?: start
                (start..maxOf(start, last + endOffsetDays)).toList()
            }.filter { it >= first && it < next }
            .distinct()
            .map(::Jdn)
    }
}

/**
 * An amanta lunar month: [start], its new moon, and the two new moons after it ([second], [third]), in days after the
 * whole Kali day [base] (that of the sankranti opening the solar month that names it).
 */
internal class LunarMonth(
    private val base: Long,
    private val start: Double,
    private val second: Double,
    private val third: Double,
) {
    /** The civil day (JDN) of absolute tithi [target] (1‥60, counted from the start of this month). */
    fun dayOf(
        target: Int,
        observance: TithiObservance,
    ): Long {
        val firstJdn = baseJdn() + floor(start).toLong() - 1
        val reached =
            (0 until SCAN_DAYS)
                .asSequence()
                .map { firstJdn + it }
                .first { tithiAt(observedAt(it, observance)) >= target }
        return if (tithiAt(observedAt(reached, observance)) == target) reached else reached - 1
    }

    private fun baseJdn(): Long = base + NepaliMonthStarts.KALI_EPOCH_JDN

    /** Days after [base] of [observance] on civil day [jdn]. */
    private fun observedAt(
        jdn: Long,
        observance: TithiObservance,
    ): Double {
        val fraction =
            when (observance) {
                TithiObservance.SUNRISE -> KathmanduDaylight.sunrise(jdn)
                TithiObservance.SUNSET -> KathmanduDaylight.sunset(jdn)
                TithiObservance.MIDNIGHT -> 1.0
            }
        return (jdn - baseJdn()) + fraction - NepaliMonthStarts.NEPAL_AFTER_UJJAIN
    }

    /** Absolute tithi at [offset]: 0 before the month, 1‥30 in it, 31‥60 in the next month, 61 after that. */
    private fun tithiAt(offset: Double): Int {
        val lunations =
            when {
                offset < start -> return 0
                offset < second -> 0
                offset < third -> 1
                else -> return 2 * NepaliLunarDays.TITHIS_IN_MONTH + 1
            }
        val index = floor(SuryaSiddhantaMoon.elongation(base, offset) / TITHI_DEGREES).toInt()
        return lunations * NepaliLunarDays.TITHIS_IN_MONTH + minOf(index, NepaliLunarDays.TITHIS_IN_MONTH - 1) + 1
    }

    companion object {
        private const val MONTHS = 12
        private const val SCAN_DAYS = 70
        private const val TITHI_DEGREES = 12.0
        private const val NEW_MOON_STEP_DAYS = 0.5
        private const val BISECTIONS = 48

        /** The lunar month named [month] in [year], or `null` when its solar month holds no new moon (kshaya). */
        fun of(
            year: Long,
            month: Int,
        ): LunarMonth? {
            val base = NepaliMonthStarts.sankranti(year, month).jdn - NepaliMonthStarts.KALI_EPOCH_JDN
            val last = newMoonsIn(year, month, base).lastOrNull() ?: return null
            val second = newMoonAfter(base, last + 1)
            return LunarMonth(base, last, second, newMoonAfter(base, second + 1))
        }

        /** How many new moons solar [month] of [year] holds: 0 (kshaya), 1, or 2 (the first month is adhika). */
        fun newMoonCount(
            year: Long,
            month: Int,
        ): Int {
            val base = NepaliMonthStarts.sankranti(year, month).jdn - NepaliMonthStarts.KALI_EPOCH_JDN
            return newMoonsIn(year, month, base).size
        }

        /** The new moons within solar [month] of [year], in days after Kali day [base]. */
        private fun newMoonsIn(
            year: Long,
            month: Int,
            base: Long,
        ): List<Double> {
            val opening = NepaliMonthStarts.sankranti(year, month)
            val closing =
                if (month == MONTHS) {
                    NepaliMonthStarts.sankranti(year + 1, 1)
                } else {
                    NepaliMonthStarts.sankranti(year, month + 1)
                }
            val until = offset(closing, base)
            return generateSequence(newMoonAfter(base, offset(opening, base))) { newMoonAfter(base, it + 1) }
                .takeWhile { it < until }
                .toList()
        }

        /** Days after Kali day [base] of the Nepal [instant]. */
        private fun offset(
            instant: NepalInstant,
            base: Long,
        ): Double =
            (instant.jdn - NepaliMonthStarts.KALI_EPOCH_JDN - base) + instant.fraction -
                NepaliMonthStarts.NEPAL_AFTER_UJJAIN

        /** The first new moon after [from], in days after Kali day [base]. */
        private fun newMoonAfter(
            base: Long,
            from: Double,
        ): Double {
            var low = from
            var lowElongation = SuryaSiddhantaMoon.elongation(base, low)
            var high = low + NEW_MOON_STEP_DAYS
            var highElongation = SuryaSiddhantaMoon.elongation(base, high)
            // The elongation only grows (the Moon is always faster than the Sun) until it wraps at the new moon.
            while (highElongation >= lowElongation) {
                low = high
                lowElongation = highElongation
                high += NEW_MOON_STEP_DAYS
                highElongation = SuryaSiddhantaMoon.elongation(base, high)
            }
            repeat(BISECTIONS) {
                val middle = (low + high) / 2
                if (SuryaSiddhantaMoon.elongation(base, middle) >= lowElongation) low = middle else high = middle
            }
            return high
        }
    }
}
