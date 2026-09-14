/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.work.WorkManager
import ir.taqvim.core.calendar.ClockTodayProvider
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.DeviceEventDao
import ir.taqvim.data.database.IcsSubscriptionDao
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.WorkdayProfileDao
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.devicecalendar.CalendarInstancesSource
import ir.taqvim.data.devicecalendar.DeviceCalendarRepository
import ir.taqvim.data.devicecalendar.InstancesSource
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.eventsDataModule
import ir.taqvim.data.events.ics.SubscriptionRefreshScheduler
import ir.taqvim.data.events.ics.SubscriptionRefresher
import ir.taqvim.data.events.ics.icsDataModule
import ir.taqvim.data.location.DeviceLocator
import ir.taqvim.data.location.PlatformGeocoder
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.AlarmDelivery
import ir.taqvim.data.scheduler.AlarmInputWatcher
import ir.taqvim.data.scheduler.AlarmScheduler
import ir.taqvim.data.scheduler.AlarmSource
import ir.taqvim.data.scheduler.PreferenceChangeWatcher
import ir.taqvim.data.scheduler.schedulerModule
import ir.taqvim.feature.agenda.AgendaDaySource
import ir.taqvim.feature.agenda.AgendaSettingsSource
import ir.taqvim.feature.agenda.agendaFeatureModule
import ir.taqvim.feature.astronomy.AstronomySettingsSource
import ir.taqvim.feature.astronomy.astronomyFeatureModule
import ir.taqvim.feature.calendar.CalendarDaySource
import ir.taqvim.feature.calendar.CalendarDisplayStore
import ir.taqvim.feature.calendar.CalendarMonthSource
import ir.taqvim.feature.calendar.CalendarPlaceSource
import ir.taqvim.feature.calendar.CalendarSettingsSource
import ir.taqvim.feature.calendar.EventSearchSource
import ir.taqvim.feature.calendar.OfficialReminderStore
import ir.taqvim.feature.calendar.calendarFeatureModule
import ir.taqvim.feature.compass.CompassSettingsSource
import ir.taqvim.feature.compass.LevelCalibrationStore
import ir.taqvim.feature.compass.compassFeatureModule
import ir.taqvim.feature.events.EditorSettingsSource
import ir.taqvim.feature.events.PersonalEventStore
import ir.taqvim.feature.events.eventsFeatureModule
import ir.taqvim.feature.notification.AthanAlarms
import ir.taqvim.feature.notification.AthanSetupSource
import ir.taqvim.feature.notification.ReminderAlarms
import ir.taqvim.feature.notification.ReminderSetupSource
import ir.taqvim.feature.notification.notificationFeatureModule
import ir.taqvim.feature.search.RecentQueriesStore
import ir.taqvim.feature.search.SearchEventSource
import ir.taqvim.feature.search.SearchSettingsSource
import ir.taqvim.feature.search.searchFeatureModule
import ir.taqvim.feature.settings.AthanPreview
import ir.taqvim.feature.settings.AthanSettingsStore
import ir.taqvim.feature.settings.AthanSoundLibrary
import ir.taqvim.feature.settings.CitySearch
import ir.taqvim.feature.settings.DeviceLocation
import ir.taqvim.feature.settings.ExactAlarmAccess
import ir.taqvim.feature.settings.GeneralSettingsStore
import ir.taqvim.feature.settings.LocationSettingsStore
import ir.taqvim.feature.settings.PlaceDescriber
import ir.taqvim.feature.settings.SubscriptionsStore
import ir.taqvim.feature.settings.athanSettingsFeatureModule
import ir.taqvim.feature.settings.generalSettingsFeatureModule
import ir.taqvim.feature.settings.locationSettingsFeatureModule
import ir.taqvim.feature.timeline.TimelineClockSource
import ir.taqvim.feature.timeline.TimelineDaysSource
import ir.taqvim.feature.timeline.TimelinePlaceSource
import ir.taqvim.feature.timeline.TimelineSettingsSource
import ir.taqvim.feature.timeline.timelineFeatureModule
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
import org.koin.core.qualifier.named
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
        single { get<TaqvimDatabase>().officialReminderDao() }
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
        single<CalendarDisplayStore> { PreferencesCalendarDisplayStore(get()) }
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
        single<LevelCalibrationStore> { PreferencesLevelCalibrationStore(get()) }
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

/** Ports of search (T-804), the timeline (T-900) and the athan settings (T-1101) over the data layer. */
val searchTimelineAthanPortsModule =
    module {
        single<SearchSettingsSource> { PreferencesSearchSettingsSource(get()) }
        single<SearchEventSource> {
            val preferences = get<UserPreferencesRepository>()
            val personal = get<PersonalEventDao>()
            val devices = get<DeviceEventDao>()
            val feeds = get<IcsSubscriptionDao>()
            CompositeSearchEventSource(
                official = OfficialEventSearchSource(language = { preferences.currentLanguage() }, today = get()),
                stores =
                    SearchEventStores(
                        personal = { personal.all() },
                        device = { from, to -> devices.observeInRange(from, to).first() },
                        subscriptions = { from, to -> feeds.observeEvents(from, to).first() },
                    ),
                today = get(),
            )
        }
        single<RecentQueriesStore> { PreferencesRecentQueriesStore(get()) }
        single<TimelineSettingsSource> { PreferencesTimelineSettingsSource(get()) }
        single<TimelineDaysSource> {
            val events = get<EventsRepository>()
            RepositoryTimelineDaysSource(
                events::days,
                get<UserPreferencesRepository>().preferences.map { it.languageCode },
            )
        }
        single<TimelinePlaceSource> { TimesTimelinePlaceSource(get()) }
        single<TimelineClockSource> { DeviceTimelineClockSource(get()) }
        single<AthanSettingsStore> { PreferencesAthanSettingsStore(get()) }
        single<ExactAlarmAccess> { SchedulerExactAlarmAccess(get<AlarmScheduler>().exactAlarmStatus) }
        single<AthanSoundLibrary> { ContentResolverAthanSoundLibrary(get()) }
        single<AthanPreview> { MediaPlayerAthanPreview(androidContext()) }
    }

/** The athan (T-1102) over the preferences, and its alarms as the scheduler's prayer source and delivery (T-604). */
val athanAlarmPortsModule =
    module {
        single<AthanSetupSource> { PreferencesAthanSetupSource(get()) }
        single<AlarmSource>(named(PRAYER_ALARMS)) {
            val alarms = get<AthanAlarms>()
            AthanAlarmSource { alarms.upcoming(it) }
        }
        single<AlarmDelivery>(named(PRAYER_ALARMS)) {
            val alarms = get<AthanAlarms>()
            AthanAlarmDelivery { alarms.onAlarm(it) }
        }
    }

/**
 * Reminders (T-1001, T-1002) over the Room personal events and official opt-ins: the scheduler's reminder source and
 * delivery (T-604), and the watcher that recomputes reminder alarms when that data changes.
 */
val reminderAlarmPortsModule =
    module {
        single<ReminderSetupSource> { RoomReminderSetupSource(get(), get(), get(), get()) }
        single<OfficialReminderStore> { RoomOfficialReminderStore(get()) }
        single { AlarmInputWatcher(reminderInputChanges(get()), setOf(AlarmKind.REMINDER), get()) }
        single<AlarmSource>(named(REMINDER_ALARMS)) {
            val alarms = get<ReminderAlarms>()
            ReminderAlarmSource { alarms.upcoming(it) }
        }
        single<AlarmDelivery>(named(REMINDER_ALARMS)) {
            val alarms = get<ReminderAlarms>()
            ReminderAlarmDelivery { id, at -> alarms.onAlarm(id, at) }
        }
    }

/** The settings screens (T-1500) and calendar subscriptions (T-1003) over the preferences and the subscription DAO. */
val settingsPortsModule =
    module {
        single<GeneralSettingsStore> {
            val preferences = get<UserPreferencesRepository>()
            val scheduler = get<SubscriptionRefreshScheduler>()
            PreferencesGeneralSettingsStore(preferences, subscriptionRescheduler(get(), preferences, scheduler::update))
        }
        single<SubscriptionsStore> {
            val preferences = get<UserPreferencesRepository>()
            val scheduler = get<SubscriptionRefreshScheduler>()
            val refresher = get<SubscriptionRefresher>()
            RoomSubscriptionsStore(
                dao = get(),
                refresh = { refresher.refresh(it) },
                preferences = preferences,
                reschedule = subscriptionRescheduler(get(), preferences, scheduler::update),
            )
        }
    }

/** Qualifiers of the scheduler's alarm kinds; every source and delivery is collected with `getAll()`. */
internal const val PRAYER_ALARMS = "prayer"
internal const val REMINDER_ALARMS = "reminder"

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
            searchTimelineAthanPortsModule,
            searchFeatureModule,
            timelineFeatureModule,
            athanSettingsFeatureModule,
            notificationFeatureModule,
            athanAlarmPortsModule,
            reminderAlarmPortsModule,
            settingsPortsModule,
            generalSettingsFeatureModule,
        )
    }
