/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.work.WorkManager
import ir.taqvim.core.calendar.ClockTodayProvider
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.WorkdayProfileDao
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.devicecalendar.CalendarInstancesSource
import ir.taqvim.data.devicecalendar.DeviceCalendarRepository
import ir.taqvim.data.devicecalendar.InstancesSource
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.eventsDataModule
import ir.taqvim.data.events.ics.icsDataModule
import ir.taqvim.data.location.DeviceLocator
import ir.taqvim.data.location.PlatformGeocoder
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.PreferenceChangeWatcher
import ir.taqvim.data.scheduler.schedulerModule
import ir.taqvim.feature.agenda.AgendaDaySource
import ir.taqvim.feature.agenda.AgendaSettingsSource
import ir.taqvim.feature.agenda.agendaFeatureModule
import ir.taqvim.feature.astronomy.AstronomySettingsSource
import ir.taqvim.feature.astronomy.astronomyFeatureModule
import ir.taqvim.feature.calendar.CalendarDaySource
import ir.taqvim.feature.calendar.CalendarMonthSource
import ir.taqvim.feature.calendar.CalendarPlaceSource
import ir.taqvim.feature.calendar.CalendarSettingsSource
import ir.taqvim.feature.calendar.EventSearchSource
import ir.taqvim.feature.calendar.calendarFeatureModule
import ir.taqvim.feature.compass.CompassSettingsSource
import ir.taqvim.feature.compass.LevelCalibrationStore
import ir.taqvim.feature.compass.compassFeatureModule
import ir.taqvim.feature.events.EditorSettingsSource
import ir.taqvim.feature.events.PersonalEventStore
import ir.taqvim.feature.events.eventsFeatureModule
import ir.taqvim.feature.settings.CitySearch
import ir.taqvim.feature.settings.DeviceLocation
import ir.taqvim.feature.settings.LocationSettingsStore
import ir.taqvim.feature.settings.PlaceDescriber
import ir.taqvim.feature.settings.locationSettingsFeatureModule
import ir.taqvim.feature.times.TimesSettingsSource
import ir.taqvim.feature.times.timesFeatureModule
import ir.taqvim.feature.tools.ToolsSettingsSource
import ir.taqvim.feature.tools.toolsFeatureModule
import ir.taqvim.feature.year.YearDaysSource
import ir.taqvim.feature.year.YearSettingsSource
import ir.taqvim.feature.year.YearTodaySource
import ir.taqvim.feature.year.yearFeatureModule
import java.util.Locale
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Declared before appModule: top-level properties initialise in file order and `includes` needs these values.

/** Platform, storage and data bindings that only the app can wire (ADR-0002). */
val appDataModule =
    module {
        single<Clock> { Clock.System }
        single<TodayProvider> { ClockTodayProvider(get()) { TimeZone.currentSystemDefault() } }
        single { TaqvimDatabase.build(androidContext()) }
        single { get<TaqvimDatabase>().personalEventDao() }
        single { get<TaqvimDatabase>().reminderDao() }
        single { get<TaqvimDatabase>().workdayProfileDao() }
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
        // T-1003: iCalendar documents and periodic subscription refresh; the worker factory is installed by the
        // application's WorkManager configuration.
        single { androidContext().contentResolver }
        single { WorkManager.getInstance(androidContext()) }
        // T-604: preference changes that move alarm times reach the scheduler (started by the application).
        single { PreferenceChangeWatcher(get<UserPreferencesRepository>().preferences, get()) }
        // T-605: backup and restore of personal data and preferences (UI in T-1503).
        single { BackupService(get(), get()) }
        // T-603: the bundled city catalog, parsed on first use.
        single { CityCatalogProvider() }
    }

/** Feature ports over the data layer (T-800…T-805, T-901, T-1000, T-1100, T-1300, T-1302/T-1303, T-1400, T-1502). */
val appFeaturePortsModule =
    module {
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
        single<TimesSettingsSource> { PreferencesTimesSettingsSource(get(), get()) }
        single<CalendarPlaceSource> { TimesCalendarPlaceSource(get()) }
        single<PersonalEventStore> {
            val preferences = get<UserPreferencesRepository>()
            RoomPersonalEventStore(get(), get(), get(), get()) { preferences.preferences.first().availableArithmetic() }
        }
        single<EditorSettingsSource> { PreferencesEditorSettingsSource(get(), OfficialAnchorLookup()) }
        single<AstronomySettingsSource> { TimesAstronomySettingsSource(get()) }
        single<CompassSettingsSource> { PreferencesCompassSettingsSource(get(), get()) }
        single<LevelCalibrationStore> { SessionLevelCalibrationStore() }
        single<ToolsSettingsSource> {
            val profiles = get<WorkdayProfileDao>().observeAll()
            PreferencesToolsSettingsSource(get(), profiles.map { it.defaultProfile() })
        }
        single<YearSettingsSource> { PreferencesYearSettingsSource(get()) }
        single<YearDaysSource> {
            val events = get<EventsRepository>()
            RepositoryYearDaysSource(events::days)
        }
        single<YearTodaySource> { TickingYearTodaySource(get()) }
        single<AgendaSettingsSource> { PreferencesAgendaSettingsSource(get()) }
        single<AgendaDaySource> {
            val events = get<EventsRepository>()
            RepositoryAgendaDaySource(
                events::days,
                get<UserPreferencesRepository>().preferences.map { it.languageCode },
            )
        }
        single<LocationSettingsStore> { PreferencesLocationSettingsStore(get()) }
        single<CitySearch> {
            val preferences = get<UserPreferencesRepository>()
            CatalogCitySearch(get()) { preferences.currentLanguage() }
        }
        single<DeviceLocation> {
            val locator = DeviceLocator(androidContext())
            LocatorDeviceLocation { locator.currentLocation() }
        }
        single<PlaceDescriber> {
            val preferences = get<UserPreferencesRepository>()
            val geocoder = PlatformGeocoder(androidContext(), Locale.getDefault())
            GeocoderPlaceDescriber({ geocoder.placesAt(it) }, get(), { preferences.currentLanguage() })
        }
    }

/** Root Koin module. Feature and data modules contribute their bindings here as they are implemented. */
val appModule =
    module {
        includes(
            appDataModule,
            appFeaturePortsModule,
            eventsDataModule,
            icsDataModule,
            schedulerModule,
            calendarFeatureModule,
            timesFeatureModule,
            eventsFeatureModule,
            astronomyFeatureModule,
            compassFeatureModule,
            toolsFeatureModule,
            yearFeatureModule,
            agendaFeatureModule,
            locationSettingsFeatureModule,
        )
    }
