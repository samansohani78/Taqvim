/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.floor
import kotlin.time.Instant

/** Half of the lunar month a tithi belongs to. */
public enum class Paksha {
    /** Waxing: tithis 1‥15. */
    SHUKLA,

    /** Waning: tithis 16‥30. */
    KRISHNA,
}

/** Tithi [number] (1‥30) and its [paksha]; tithi 15 is Purnima (full moon) and 30 Amavasya (new moon). */
public data class TithiPosition(
    public val number: Int,
    public val paksha: Paksha,
) {
    /** Number within its paksha, 1‥15. */
    public val numberInPaksha: Int
        get() = (number - 1) % TITHIS_PER_PAKSHA + 1

    private companion object {
        const val TITHIS_PER_PAKSHA = 15
    }
}

/**
 * The tithi of the Surya Siddhanta (ADR-0038, DT-043): the convention the app's own Nepali festivals are computed
 * with, and therefore the one it displays.
 *
 * A tithi is a twelfth of a lap of the Moon's elongation from the Sun, and the two conventions the app carries
 * disagree about where that lap is. [ir.taqvim.core.astronomy.Tithi] reads a modern ephemeris; this one reads
 * [SuryaSiddhantaMoon] and [SuryaSiddhantaSun]. ADR-0038 measured the difference: the modern ephemeris misses two of
 * the twenty-one dated lunar holidays of the BS 2082 and 2083 Rajpatra notices, where the Surya Siddhanta reproduces
 * every one, because Nepal's own panchang follows it. Showing the other value would put a tithi on screen that
 * disagrees with the festival this same app places on another day.
 *
 * Time is the Surya Siddhanta's: mean solar time at Ujjain, 75°46′ E, counted in days from the Kali epoch
 * ([NepaliMonthStarts.KALI_EPOCH_JDN]).
 */
public object SuryaSiddhantaTithi {
    private const val TITHI_DEGREES = 12.0
    private const val TITHIS_PER_MONTH = 30
    private const val TITHIS_PER_PAKSHA = 15
    private const val UJJAIN_LONGITUDE_DEGREES = 75.0 + 46.0 / 60.0
    private const val DEGREES_IN_A_TURN = 360.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Julian Date of 1970-01-01T00:00Z, for turning an instant into the day count the engine takes. */
    private const val JULIAN_DATE_OF_UNIX_EPOCH = 2_440_587.5

    /** The tithi current at [instant]. */
    public fun at(instant: Instant): TithiPosition = ofElongation(elongationAt(instant))

    /** The Moon's elongation east of the Sun at [instant], in degrees 0‥360, by the Surya Siddhanta. */
    public fun elongationAt(instant: Instant): Double {
        val ujjainDays = ujjainDaysSinceKaliEpoch(instant)
        val whole = floor(ujjainDays)
        return SuryaSiddhantaMoon.elongation(whole.toLong(), ujjainDays - whole)
    }

    /** Tithi for a Moon–Sun elongation of [elongationDegrees] (any real value). */
    public fun ofElongation(elongationDegrees: Double): TithiPosition {
        val wrapped = elongationDegrees.mod(DEGREES_IN_A_TURN)
        val number = (wrapped / TITHI_DEGREES).toInt().coerceAtMost(TITHIS_PER_MONTH - 1) + 1
        return TithiPosition(number, if (number <= TITHIS_PER_PAKSHA) Paksha.SHUKLA else Paksha.KRISHNA)
    }

    /**
     * Days of Ujjain mean solar time between the Kali epoch and [instant].
     *
     * The same quantity `NepaliLunarDays` computes as `(jdn - kaliEpoch) + localFraction - NEPAL_AFTER_UJJAIN`, from
     * an instant instead of a Nepali civil day and a fraction of it.
     */
    private fun ujjainDaysSinceKaliEpoch(instant: Instant): Double {
        val julianDate = instant.toEpochMilliseconds() / MILLIS_PER_DAY + JULIAN_DATE_OF_UNIX_EPOCH
        val ujjainOffsetDays = UJJAIN_LONGITUDE_DEGREES / DEGREES_IN_A_TURN
        return julianDate + HALF_DAY + ujjainOffsetDays - NepaliMonthStarts.KALI_EPOCH_JDN
    }

    /** A Julian Date starts at noon and a Julian day number at the midnight before it. */
    private const val HALF_DAY = 0.5
}
