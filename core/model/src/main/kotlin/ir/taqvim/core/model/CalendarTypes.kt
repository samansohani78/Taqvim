/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

/** Calendar systems supported by Taqvim (docs/PLAN.md §0.3). */
public enum class CalendarSystem {
    /** Solar Hijri (Persian), astronomical year start at the Tehran meridian. */
    PERSIAN,

    /** Lunar Hijri; the concrete rule is selected by [IslamicVariant]. */
    ISLAMIC,

    /** Proleptic Gregorian. */
    GREGORIAN,

    /** Bikram Sambat (Nepal), computed from sankrantis (A-07, ADR-0030). */
    NEPALI,

    /** Fixed Hebrew calendar (A-15, T-108): 12 or 13 months numbered from Tishri (ADR-0025 addendum). */
    HEBREW,
}

/** Rules for determining Islamic (Lunar Hijri) month starts (docs/PLAN.md §6, A-03…A-06). */
public enum class IslamicVariant {
    /** Official Iranian dates from published override tables on top of the tabular calendar. */
    IRAN_OFFICIAL,

    /** Saudi Umm al-Qura calendar. */
    UMM_AL_QURA,

    /** Arithmetic 30-year cycle with leap years 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29 (type II). */
    TABULAR_16,

    /** Arithmetic 30-year cycle with leap years 2, 5, 7, 10, 13, 15, 18, 21, 24, 26, 29 (type I). */
    TABULAR_15,

    /** Crescent-visibility estimate (Yallop criterion) at the user's location. */
    CALCULATED_OBSERVATIONAL,
}

/**
 * A day expressed in a [system]: [year], 1-based [month] and 1-based [day]. Only the structural invariants are
 * checked here; whether the date exists in its calendar is decided by `:core:calendar`.
 */
public data class CalendarDate(
    public val system: CalendarSystem,
    public val year: Int,
    public val month: Int,
    public val day: Int,
) {
    init {
        require(month >= 1) { "month must be ≥ 1 (was $month)" }
        require(day >= 1) { "day must be ≥ 1 (was $day)" }
    }

    /** `YYYY-MM-DD` in the date's own calendar (years below 1000 are zero-padded; negative years keep the sign). */
    public fun toIsoLikeString(): String {
        val sign = if (year < 0) "-" else ""
        val absoluteYear = Math.abs(year.toLong()).toString().padStart(YEAR_DIGITS, '0')
        return "$sign$absoluteYear-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    override fun toString(): String = "$system ${toIsoLikeString()}"

    private companion object {
        const val YEAR_DIGITS = 4
    }
}

/** Day of week in ISO order (Monday first). Presentation applies the user's week start. */
public enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ;

    /** ISO-8601 day number: Monday = 1 … Sunday = 7. */
    public val isoNumber: Int
        get() = ordinal + 1

    /** The weekday [days] after this one (negative values go backwards). */
    public operator fun plus(days: Int): Weekday = entries[Math.floorMod(ordinal + days, entries.size)]

    /** Number of days from [other] forward to this weekday, in `0..6`. */
    public fun daysAfter(other: Weekday): Int = Math.floorMod(ordinal - other.ordinal, entries.size)

    public companion object {
        /** The weekday with ISO number [isoNumber] (1 = Monday … 7 = Sunday). */
        public fun ofIsoNumber(isoNumber: Int): Weekday {
            require(isoNumber in 1..entries.size) { "ISO weekday must be in 1..7 (was $isoNumber)" }
            return entries[isoNumber - 1]
        }
    }
}

/** A position on Earth in degrees (WGS 84) with [elevationMeters] above sea level. */
public data class Coordinates(
    public val latitude: Double,
    public val longitude: Double,
    public val elevationMeters: Double = 0.0,
) {
    init {
        require(latitude.isFinite() && latitude in -MAX_LATITUDE..MAX_LATITUDE) {
            "latitude must be within ±90° (was $latitude)"
        }
        require(longitude.isFinite() && longitude in -MAX_LONGITUDE..MAX_LONGITUDE) {
            "longitude must be within ±180° (was $longitude)"
        }
        require(elevationMeters.isFinite()) { "elevation must be finite (was $elevationMeters)" }
    }

    private companion object {
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0
    }
}

/** A time of day with minute precision, `0..1439`; integer-based so prayer times carry no floating error. */
@JvmInline
public value class MinuteOfDay(
    public val value: Int,
) : Comparable<MinuteOfDay> {
    init {
        require(value in 0 until MINUTES_PER_DAY) { "minute of day must be in 0..1439 (was $value)" }
    }

    /** Hour of day, `0..23`. */
    public val hour: Int
        get() = value / MINUTES_PER_HOUR

    /** Minute of hour, `0..59`. */
    public val minute: Int
        get() = value % MINUTES_PER_HOUR

    /** This time moved by [minutes], wrapping around midnight. */
    public fun plusWrapping(minutes: Int): MinuteOfDay = MinuteOfDay(Math.floorMod(value + minutes, MINUTES_PER_DAY))

    override fun compareTo(other: MinuteOfDay): Int = value.compareTo(other.value)

    /** `HH:MM` with Latin digits (localized formatting lives in `:core:i18n`). */
    override fun toString(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    public companion object {
        /** Minutes in a day. */
        public const val MINUTES_PER_DAY: Int = 1440

        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24

        /** The time [hour]:[minute]. */
        public fun of(
            hour: Int,
            minute: Int,
        ): MinuteOfDay {
            require(hour in 0 until HOURS_PER_DAY) { "hour must be in 0..23 (was $hour)" }
            require(minute in 0 until MINUTES_PER_HOUR) { "minute must be in 0..59 (was $minute)" }
            return MinuteOfDay(hour * MINUTES_PER_HOUR + minute)
        }
    }
}
