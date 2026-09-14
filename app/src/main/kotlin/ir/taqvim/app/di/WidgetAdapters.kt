/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.preferences.StoredWidgetConfig
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.WidgetConfigRepository
import ir.taqvim.feature.map.WorldOutline
import ir.taqvim.feature.widgets.WidgetBackground
import ir.taqvim.feature.widgets.WidgetCalendarsSource
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetConfigStore
import ir.taqvim.feature.widgets.WidgetContent
import ir.taqvim.feature.widgets.WidgetContentBuilder
import ir.taqvim.feature.widgets.WidgetCountdownSource
import ir.taqvim.feature.widgets.WidgetData
import ir.taqvim.feature.widgets.WidgetDataSource
import ir.taqvim.feature.widgets.WidgetDayInputs
import ir.taqvim.feature.widgets.WidgetDependency
import ir.taqvim.feature.widgets.WidgetEventLine
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetPlace
import ir.taqvim.feature.widgets.WidgetPrayerNames
import ir.taqvim.feature.widgets.WidgetPrayers
import ir.taqvim.feature.widgets.WidgetRefresher
import ir.taqvim.feature.widgets.WidgetTimeline
import ir.taqvim.feature.widgets.WidgetTimelineSource
import ir.taqvim.feature.widgets.WidgetUpdateTrigger
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * [WidgetDataSource] (T-1201…T-1212) from the stored preferences and the events repository (T-305): the day of [now] in
 * the chosen place's zone (or the device zone), in the user's calendars and language, with its events and, where a
 * place is chosen, its prayer times; the calendar widgets add their month, week or schedule, the sky widgets their
 * Sun path, Moon or map ([WidgetSkyParts]) and the countdown widget its countdown.
 */
internal class PreferencesWidgetDataSource(
    private val preferences: UserPreferencesRepository,
    private val rangeEvents: (JdnRange) -> Flow<List<DayEvents>>,
    private val names: WidgetPrayerNames,
    private val skyParts: WidgetSkyParts = WidgetSkyParts { WorldOutline(emptyList(), emptyList()) },
    private val deviceZone: () -> TimeZone,
) : WidgetDataSource {
    private val calendarParts = WidgetCalendarParts(rangeEvents)

    override suspend fun load(
        kind: WidgetKind,
        config: WidgetConfig,
        now: Instant,
        view: WidgetView,
    ): WidgetData {
        val prefs = preferences.preferences.first()
        val place = prefs.widgetPlace()
        val zone = place?.timeZone ?: deviceZone()
        val jdn = now.toJdn(zone)
        val day = rangeEvents(jdn..jdn).first().single()
        val inputs = widgetInputs(prefs, config, jdn, day, place)
        val content = WidgetContentBuilder.build(inputs, now, names)
        return when (kind) {
            WidgetKind.SUN_ARC, WidgetKind.MOON, WidgetKind.MAP -> {
                skyParts.addTo(content, kind, inputs, now, zone)
            }

            WidgetKind.COUNTDOWN -> {
                content.copy(countdown = prefs.widgetCountdown(config, jdn, inputs.language))
            }

            else -> {
                calendarParts.addTo(content, kind, inputs, prefs.weekStart, view)
            }
        }
    }
}

/** What the widgets show for [day] under [prefs] and [config]; the secondary calendar never repeats the primary one. */
internal fun widgetInputs(
    prefs: UserPreferences,
    config: WidgetConfig,
    jdn: Jdn,
    day: DayEvents,
    place: WidgetPlace?,
): WidgetDayInputs {
    val calendars = prefs.availableCalendars()
    val primary = calendars.firstOrNull() ?: PersianCalendarSystem
    val secondary =
        (config.secondaryCalendar?.let { prefs.availableArithmetic()[it] } ?: calendars.getOrNull(1))
            ?.takeIf { it.system != primary.system }
    return WidgetDayInputs(
        jdn = jdn,
        language = prefs.languageSpec(),
        primary = primary,
        secondary = secondary,
        isHoliday = day.isHoliday,
        events = day.widgetEventLines(prefs.languageCode),
        place = place,
    )
}

/** Dataset events (holidays first) in [language], then personal (opening the editor), device and feed events. */
internal fun DayEvents.widgetEventLines(language: String): List<WidgetEventLine> =
    official.map { WidgetEventLine(it.definition.title.forLanguage(language), it.isHoliday) } +
        personal.map { WidgetEventLine(it.title, isHoliday = false, eventId = it.eventId) } +
        device.map { WidgetEventLine(it.title, isHoliday = false) } +
        ics.map { WidgetEventLine(it.summary, isHoliday = false) }

/** The chosen place with its zone and prayer conventions, or `null` without a place or with an unknown zone. */
internal fun UserPreferences.widgetPlace(): WidgetPlace? {
    val place = place ?: return null
    val zone = runCatching { TimeZone.of(place.zoneId) }.getOrNull() ?: return null
    return WidgetPlace(place.coordinates, zone, prayerSettings())
}

/** [WidgetTimelineSource]: the widgets' zone and the next prayer time at the chosen place. */
internal class PreferencesWidgetTimelineSource(
    private val preferences: UserPreferencesRepository,
    private val deviceZone: () -> TimeZone,
) : WidgetTimelineSource {
    override suspend fun timeline(now: Instant): WidgetTimeline {
        val place = preferences.preferences.first().widgetPlace()
        return WidgetTimeline(place?.timeZone ?: deviceZone(), place?.let { WidgetPrayers.next(now, it)?.second })
    }
}

/** [WidgetCalendarsSource]: the user's calendars in order. */
internal class PreferencesWidgetCalendarsSource(
    private val preferences: UserPreferencesRepository,
) : WidgetCalendarsSource {
    override fun calendars(): Flow<List<CalendarSystem>> =
        preferences.preferences.map { it.calendars.distinct() }.distinctUntilChanged()
}

/** [WidgetConfigStore] over the widget configuration DataStore; unknown stored names fall back to the defaults. */
internal class DataStoreWidgetConfigStore(
    private val repository: WidgetConfigRepository,
) : WidgetConfigStore {
    override suspend fun config(appWidgetId: Int): WidgetConfig? = repository.config(appWidgetId)?.toWidgetConfig()

    override suspend fun save(
        appWidgetId: Int,
        config: WidgetConfig,
    ) {
        repository.save(appWidgetId, config.toStored())
    }

    override suspend fun delete(appWidgetIds: Set<Int>) {
        repository.delete(appWidgetIds)
    }
}

internal fun StoredWidgetConfig.toWidgetConfig(): WidgetConfig =
    WidgetConfig(
        background = WidgetBackground.entries.firstOrNull { it.name == background } ?: WidgetBackground.SURFACE,
        transparencyPercent = transparencyPercent,
        scalePercent = scalePercent,
        contents = WidgetContent.entries.filter { it.name in contents }.toImmutableSet(),
        secondaryCalendar = secondaryCalendar,
        countdown = countdown?.toWidgetCountdown(),
    )

internal fun WidgetConfig.toStored(): StoredWidgetConfig =
    StoredWidgetConfig(
        background = background.name,
        transparencyPercent = transparencyPercent,
        scalePercent = scalePercent,
        contents = contents.mapTo(mutableSetOf()) { it.name },
        secondaryCalendar = secondaryCalendar,
        countdown = countdown?.toStored(),
    )

/** What of the widgets' content a change from [old] to [new] preferences makes stale. */
internal fun widgetDependenciesChanged(
    old: UserPreferences,
    new: UserPreferences,
): Set<WidgetDependency> =
    buildSet {
        if (old.appearanceKey() != new.appearanceKey()) add(WidgetDependency.APPEARANCE)
        if (old.place != new.place) add(WidgetDependency.LOCATION)
        if (old.place != new.place || old.prayerSettings() != new.prayerSettings()) add(WidgetDependency.PRAYER_TIMES)
        val events = old.app.enabledEventSources != new.app.enabledEventSources || old.weekend != new.weekend
        if (events || old.islamicVariant != new.islamicVariant) add(WidgetDependency.EVENTS)
    }

/** What the widgets' texts and colors depend on. */
private fun UserPreferences.appearanceKey(): List<Any?> =
    listOf(languageCode, calendars, numerals, themeMode, islamicVariant, hijriOffsetDays, app.dynamicColor)

/**
 * Keeps placed widgets current (T-1201…): preference changes redraw the widgets they affect, and changes to personal
 * events or the device and feed caches redraw event widgets (debounced). Day changes and prayer times are handled by
 * the widget framework's own wake-up; with no widget placed the refresher touches nothing.
 */
internal class WidgetTriggerWatcher(
    private val preferences: Flow<UserPreferences>,
    private val eventChanges: Flow<Set<String>>,
    private val refresher: WidgetRefresher,
    private val quietPeriod: Duration = QUIET_PERIOD,
) {
    @OptIn(FlowPreview::class)
    suspend fun watch(): Unit =
        coroutineScope {
            launch {
                preferences
                    .distinctUntilChanged()
                    .scan(PreferenceStep(null, null)) { step, current -> PreferenceStep(step.current, current) }
                    .collect { step ->
                        val affected = step.changedDependencies()
                        if (affected.isNotEmpty()) refresher.refresh(WidgetUpdateTrigger.PreferencesChanged(affected))
                    }
            }
            launch {
                eventChanges.debounce(quietPeriod).collect { refresher.refresh(WidgetUpdateTrigger.EventsChanged) }
            }
        }

    private companion object {
        val QUIET_PERIOD: Duration = 2.seconds
    }
}

/** Two consecutive preference values; nothing is stale until both are known. */
private data class PreferenceStep(
    val previous: UserPreferences?,
    val current: UserPreferences?,
) {
    fun changedDependencies(): Set<WidgetDependency> =
        if (previous == null || current == null) emptySet() else widgetDependenciesChanged(previous, current)
}

/** Invalidations of the tables whose rows widgets show. */
internal fun widgetEventChanges(database: TaqvimDatabase): Flow<Set<String>> =
    database.invalidationTracker.createFlow(
        "personal_events",
        "event_recurrences",
        "device_events_cache",
        "ics_events_cache",
        emitInitialState = false,
    )

/** Widgets (T-1201…T-1209) over the preferences, the events repository and their own configuration store. */
val widgetPortsModule =
    module {
        single {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            WidgetConfigRepository(WidgetConfigRepository.createDataStore(androidContext(), scope))
        }
        single<WidgetConfigStore> { DataStoreWidgetConfigStore(get()) }
        single<WidgetDataSource> {
            val events = get<EventsRepository>()
            PreferencesWidgetDataSource(get(), events::days, get(), WidgetSkyParts(get())) {
                TimeZone.currentSystemDefault()
            }
        }
        single<WidgetCountdownSource> {
            val events = get<EventsRepository>()
            PreferencesWidgetCountdownSource(get(), events::days, get()) { TimeZone.currentSystemDefault() }
        }
        single<WidgetTimelineSource> { PreferencesWidgetTimelineSource(get()) { TimeZone.currentSystemDefault() } }
        single<WidgetCalendarsSource> { PreferencesWidgetCalendarsSource(get()) }
        single {
            WidgetTriggerWatcher(get<UserPreferencesRepository>().preferences, widgetEventChanges(get()), get())
        }
    }
