/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.calendar.DatePeriod
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlinx.collections.immutable.ImmutableList

/** Astronomical seasons, named for the hemisphere of the chosen place (northern when none is chosen). */
enum class SeasonName {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER,
}

/** Moon phase names: 45° sectors of the Moon's elongation centred on 0°, 45°, … 315°. */
enum class MoonPhaseName {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    THIRD_QUARTER,
    WANING_CRESCENT,
}

/** The Moon at noon of a day: lit fraction (0‥1), which limb is lit, its phase name and tropical sign. */
data class MoonOverview(
    val illuminatedFraction: Float,
    val brightLimbOnRight: Boolean,
    val phase: MoonPhaseName,
    val sign: ZodiacSign,
)

/** What the Calendars tab shows about a day besides its dates (T-802). */
data class DayOverview(
    val day: Jdn,
    /** Days from today to [day]; negative before today. */
    val daysFromToday: Long,
    /** The same difference in years, months and days of the primary calendar. */
    val period: DatePeriod,
    /** Position of [day] in its week, 1 on the week start. */
    val dayOfWeek: Int,
    /** Week of the primary calendar's year (week 1 contains the first day of the year, as in the month pager). */
    val weekOfYear: Int,
    val season: SeasonName,
    val dayOfSeason: Int,
    val seasonLength: Int,
    /** The Sun's tropical sign at noon. */
    val sunSign: ZodiacSign,
    val moon: MoonOverview,
)

/** The times of a day in display order; [primary] ones can be the "next" time. */
enum class PrayerTimeKind(
    val primary: Boolean,
) {
    FAJR(true),
    SUNRISE(true),
    DHUHR(true),
    ASR(true),
    SUNSET(false),
    MAGHRIB(true),
    ISHA(true),
    MIDNIGHT(false),
}

/** One time of a day; [time] is `null` where the chosen method leaves it undefined. */
data class PrayerTimeEntry(
    val kind: PrayerTimeKind,
    val time: MinuteOfDay?,
)

/** The times of the selected day at the chosen place (T-802 Times tab). */
data class DayTimes(
    val day: Jdn,
    val placeName: String,
    val method: PrayerMethod,
    /** Every time in display order; empty when [unavailable]. */
    val entries: ImmutableList<PrayerTimeEntry>,
    /** Why there are no times (polar day or night), or `null`. */
    val unavailable: PrayerTimesResult.Reason?,
    /** The next primary time, only while [day] is today at the place. */
    val next: PrayerTimeKind?,
    /** Elapsed daylight (0 at sunrise, 1 at sunset), only while the Sun is up on [day] at the place. */
    val sunProgress: Float?,
)

/** State of the Times tab. */
sealed interface DayTimesState {
    data object Loading : DayTimesState

    /** No place is chosen, so times cannot be calculated. */
    data object NoPlace : DayTimesState

    data class Ready(
        val times: DayTimes,
    ) : DayTimesState
}
