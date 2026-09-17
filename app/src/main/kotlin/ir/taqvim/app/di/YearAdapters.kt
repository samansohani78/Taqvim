/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.TickingTodaySource
import ir.taqvim.feature.year.YearDay
import ir.taqvim.feature.year.YearDaysSource
import ir.taqvim.feature.year.YearSettings
import ir.taqvim.feature.year.YearSettingsSource
import ir.taqvim.feature.year.YearTodaySource
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The year view's preferences (T-805) from the stored user preferences (T-600). */
internal class PreferencesYearSettingsSource(
    private val preferences: UserPreferencesRepository,
) : YearSettingsSource {
    override fun settings(): Flow<YearSettings> =
        preferences.preferences
            .map {
                YearSettings(
                    it.calendars,
                    it.weekStart,
                    it.islamicVariant,
                    it.languageCode,
                    it.islamicOverride.table,
                )
            }.distinctUntilChanged()
}

/** Holiday and weekend flags for the year view (T-805) from the days of the events repository (T-305). */
internal class RepositoryYearDaysSource(
    private val days: (JdnRange) -> Flow<List<DayEvents>>,
) : YearDaysSource {
    override fun days(range: JdnRange): Flow<List<YearDay>> =
        days.invoke(range).map { days -> days.map { it.toYearDay() } }.distinctUntilChanged()
}

/** This day's flags for the year view. */
internal fun DayEvents.toYearDay(): YearDay = YearDay(jdn, isHoliday, isWeekend)

/** [YearTodaySource] that ticks like the calendar screen's today (T-800): a new day shows within [interval]. */
internal class TickingYearTodaySource(
    provider: TodayProvider,
    interval: Duration = TickingTodaySource.DEFAULT_INTERVAL,
) : YearTodaySource {
    private val ticking = TickingTodaySource(provider, interval)

    override fun today(): Flow<Jdn> = ticking.today()
}
