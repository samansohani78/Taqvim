/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.ClockTodayProvider
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.devicecalendar.CalendarInstancesSource
import ir.taqvim.data.devicecalendar.DeviceCalendarRepository
import ir.taqvim.data.devicecalendar.InstancesSource
import ir.taqvim.data.events.eventsDataModule
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.CalendarDaySource
import ir.taqvim.feature.calendar.CalendarMonthSource
import ir.taqvim.feature.calendar.CalendarPlaceSource
import ir.taqvim.feature.calendar.CalendarSettingsSource
import ir.taqvim.feature.calendar.EventSearchSource
import ir.taqvim.feature.calendar.calendarFeatureModule
import ir.taqvim.feature.times.TimesSettingsSource
import ir.taqvim.feature.times.timesFeatureModule
import java.util.Locale
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Declared before appModule: top-level properties initialise in file order and `includes` needs this value.

/** Platform, storage and feature-port bindings that only the app can wire (ADR-0002). */
val appDataModule =
    module {
        single<Clock> { Clock.System }
        single<TodayProvider> { ClockTodayProvider(get()) { TimeZone.currentSystemDefault() } }
        single { TaqvimDatabase.build(androidContext()) }
        single { get<TaqvimDatabase>().personalEventDao() }
        single { get<TaqvimDatabase>().icsSubscriptionDao() }
        single { get<TaqvimDatabase>().deviceEventDao() }
        single {
            val dataStore =
                UserPreferencesRepository.createDataStore(
                    context = androidContext(),
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                    deviceLanguage = { Locale.getDefault().language },
                )
            UserPreferencesRepository(dataStore)
        }
        single<InstancesSource> { CalendarInstancesSource(androidContext()) }
        single { DeviceCalendarRepository(get(), get()) }
        single<CalendarSettingsSource> { PreferencesCalendarSettingsSource(get()) }
        single {
            RepositoryCalendarDaySource(get(), get<UserPreferencesRepository>().preferences.map { it.languageCode })
        }
        single<CalendarDaySource> { get<RepositoryCalendarDaySource>() }
        single<CalendarMonthSource> { get<RepositoryCalendarDaySource>() }
        single<EventSearchSource> {
            val preferences = get<UserPreferencesRepository>()
            OfficialEventSearchSource(language = { preferences.preferences.first().languageCode }, today = get())
        }
        // No stored city choice exists yet (location settings arrive with T-1502), so the Times tab shows its
        // "choose a location" state.
        single<ChosenCitySource> { ChosenCitySource { flowOf(null) } }
        single<TimesSettingsSource> { PreferencesTimesSettingsSource(get(), get()) }
        single<CalendarPlaceSource> { TimesCalendarPlaceSource(get()) }
    }

/** Root Koin module. Feature and data modules contribute their bindings here as they are implemented. */
val appModule =
    module {
        includes(appDataModule, eventsDataModule, calendarFeatureModule, timesFeatureModule)
    }
