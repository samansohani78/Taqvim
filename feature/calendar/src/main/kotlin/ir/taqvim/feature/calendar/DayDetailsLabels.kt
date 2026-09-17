/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.annotation.StringRes
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.praytimes.PrayerTimesResult

/** String resources of the day-details values, one per enum constant in declaration order (T-802). */
internal object DayDetailsLabels {
    private val TABS =
        listOf(R.string.calendar_tab_calendars, R.string.calendar_tab_events, R.string.calendar_tab_times)

    private val CALENDARS =
        listOf(
            R.string.calendar_system_persian,
            R.string.calendar_system_islamic,
            R.string.calendar_system_gregorian,
            R.string.calendar_system_nepali,
            R.string.calendar_system_hebrew,
        )

    private val ORIGINS =
        listOf(
            R.string.calendar_origin_computed,
            R.string.calendar_origin_official,
            R.string.calendar_origin_published,
        )

    private val SEASONS =
        listOf(
            R.string.calendar_season_spring,
            R.string.calendar_season_summer,
            R.string.calendar_season_autumn,
            R.string.calendar_season_winter,
        )

    private val SIGNS =
        listOf(
            R.string.calendar_sign_aries,
            R.string.calendar_sign_taurus,
            R.string.calendar_sign_gemini,
            R.string.calendar_sign_cancer,
            R.string.calendar_sign_leo,
            R.string.calendar_sign_virgo,
            R.string.calendar_sign_libra,
            R.string.calendar_sign_scorpio,
            R.string.calendar_sign_sagittarius,
            R.string.calendar_sign_capricorn,
            R.string.calendar_sign_aquarius,
            R.string.calendar_sign_pisces,
        )

    private val PHASES =
        listOf(
            R.string.calendar_phase_new_moon,
            R.string.calendar_phase_waxing_crescent,
            R.string.calendar_phase_first_quarter,
            R.string.calendar_phase_waxing_gibbous,
            R.string.calendar_phase_full_moon,
            R.string.calendar_phase_waning_gibbous,
            R.string.calendar_phase_third_quarter,
            R.string.calendar_phase_waning_crescent,
        )

    private val SOURCES =
        listOf(
            R.string.calendar_source_iran_official,
            R.string.calendar_source_afghanistan_official,
            R.string.calendar_source_nepal_official,
            R.string.calendar_source_international,
            R.string.calendar_source_ancient_iran,
            R.string.calendar_source_personal,
        )

    private val KINDS =
        listOf(
            R.string.calendar_source_official,
            R.string.calendar_source_personal,
            R.string.calendar_source_device,
            R.string.calendar_source_subscription,
        )

    private val METHODS =
        listOf(
            R.string.calendar_method_mwl,
            R.string.calendar_method_isna,
            R.string.calendar_method_egypt,
            R.string.calendar_method_makkah,
            R.string.calendar_method_karachi,
            R.string.calendar_method_tehran,
            R.string.calendar_method_jafari,
            R.string.calendar_method_singapore,
            R.string.calendar_method_france,
            R.string.calendar_method_russia,
            R.string.calendar_method_diyanet,
        )

    private val TIMES =
        listOf(
            R.string.calendar_time_fajr,
            R.string.calendar_time_sunrise,
            R.string.calendar_time_dhuhr,
            R.string.calendar_time_asr,
            R.string.calendar_time_sunset,
            R.string.calendar_time_maghrib,
            R.string.calendar_time_isha,
            R.string.calendar_time_midnight,
        )

    private val UNAVAILABLE = listOf(R.string.calendar_polar_day, R.string.calendar_polar_night)

    /** Every table above with the number of constants it must cover, for the size check in tests. */
    internal val TABLES: List<Pair<List<Int>, Int>> =
        listOf(
            TABS to DayDetailsTab.entries.size,
            CALENDARS to CalendarSystem.entries.size,
            SEASONS to SeasonName.entries.size,
            SIGNS to ZodiacSign.entries.size,
            PHASES to MoonPhaseName.entries.size,
            SOURCES to EventSource.entries.size,
            KINDS to DayEventKind.entries.size,
            METHODS to PrayerMethod.entries.size,
            TIMES to PrayerTimeKind.entries.size,
            UNAVAILABLE to PrayerTimesResult.Reason.entries.size,
        )

    @StringRes
    fun of(origin: DateOrigin): Int = ORIGINS[origin.ordinal]

    @StringRes
    fun of(tab: DayDetailsTab): Int = TABS[tab.ordinal]

    @StringRes
    fun of(system: CalendarSystem): Int = CALENDARS[system.ordinal]

    @StringRes
    fun of(season: SeasonName): Int = SEASONS[season.ordinal]

    @StringRes
    fun of(sign: ZodiacSign): Int = SIGNS[sign.ordinal]

    @StringRes
    fun of(phase: MoonPhaseName): Int = PHASES[phase.ordinal]

    @StringRes
    fun of(source: EventSource): Int = SOURCES[source.ordinal]

    @StringRes
    fun of(kind: DayEventKind): Int = KINDS[kind.ordinal]

    @StringRes
    fun of(method: PrayerMethod): Int = METHODS[method.ordinal]

    @StringRes
    fun of(time: PrayerTimeKind): Int = TIMES[time.ordinal]

    @StringRes
    fun of(reason: PrayerTimesResult.Reason): Int = UNAVAILABLE[reason.ordinal]
}
