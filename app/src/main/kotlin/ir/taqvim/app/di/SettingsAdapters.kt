/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.ui.theme.ThemeMode as UiThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.data.database.IcsCacheSummary
import ir.taqvim.data.database.IcsSubscriptionDao
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.SubscriptionErrorCodes
import ir.taqvim.data.events.ics.RefreshError
import ir.taqvim.data.events.ics.RefreshOutcome
import ir.taqvim.data.events.ics.SubscriptionRefreshPolicy
import ir.taqvim.data.events.ics.SubscriptionUrl
import ir.taqvim.data.events.ics.SubscriptionUrls
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.LevelOffset
import ir.taqvim.data.preferences.ThemeMode
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.withEventSourcesFor
import ir.taqvim.feature.compass.DeviceOrientation
import ir.taqvim.feature.compass.LevelCalibration
import ir.taqvim.feature.compass.LevelCalibrationStore
import ir.taqvim.feature.compass.Tilt
import ir.taqvim.feature.search.RecentQueriesStore
import ir.taqvim.feature.search.SearchMatcher
import ir.taqvim.feature.settings.GeneralSettings
import ir.taqvim.feature.settings.GeneralSettingsData
import ir.taqvim.feature.settings.GeneralSettingsStore
import ir.taqvim.feature.settings.SubscriptionError
import ir.taqvim.feature.settings.SubscriptionHealthData
import ir.taqvim.feature.settings.SubscriptionItem
import ir.taqvim.feature.settings.SubscriptionOutcome
import ir.taqvim.feature.settings.SubscriptionsStore
import ir.taqvim.feature.settings.ThemeChoice
import java.net.URI
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** [GeneralSettingsStore] (T-1500) over the user preferences; [afterUpdate] runs after every stored change. */
internal class PreferencesGeneralSettingsStore(
    private val preferences: UserPreferencesRepository,
    private val afterUpdate: suspend () -> Unit = {},
) : GeneralSettingsStore {
    override fun settings(): Flow<GeneralSettingsData> =
        preferences.preferences
            .map { GeneralSettingsData(it.languageSpec(), it.toGeneralSettings()) }
            .distinctUntilChanged()

    /** A changed language applies that language's defaults to everything not chosen yet (T-1501, ADR-0023). */
    override suspend fun update(transform: (GeneralSettings) -> GeneralSettings) {
        preferences.update { current ->
            val next = transform(current.toGeneralSettings())
            if (next.languageCode != current.languageCode) {
                current.withLanguage(next.languageCode)
            } else {
                current.withGeneralSettings(next)
            }
        }
        afterUpdate()
    }

    override suspend fun clearRecentSearches() {
        preferences.update { it.copy(app = it.app.copy(recentSearches = emptyList())) }
    }
}

/** These preferences as the settings screens' model. */
internal fun UserPreferences.toGeneralSettings(): GeneralSettings =
    GeneralSettings(
        languageCode = languageCode,
        theme = ThemeChoice.valueOf(themeMode.name),
        dynamicColor = app.dynamicColor,
        highContrast = app.highContrast,
        boldText = app.boldText,
        gradient = app.gradient,
        numerals = numerals,
        calendars = calendars,
        weekStart = weekStart,
        weekend = weekend,
        islamicVariant = islamicVariant,
        showWeekNumbers = app.showWeekNumbers,
        enabledEventSources = app.enabledEventSources,
        prayerMethod = prayerMethod,
        asrJuristic = asrJuristic,
        highLatitudeRule = app.highLatitudeRule,
        subscriptionsNetworkAllowed = app.subscriptionsNetworkAllowed,
        persistentNotification = app.persistentNotification,
        persistentNotificationLargeNumber = app.persistentNotificationLargeNumber,
        dynamicLauncherIcon = app.dynamicLauncherIcon,
        rememberRecentSearches = app.rememberRecentSearches,
        hasRecentSearches = app.recentSearches.isNotEmpty(),
        allDayReminderMinute = app.allDayReminderMinute,
    )

/**
 * These preferences with [settings] applied; turning search history off forgets the stored searches. Event sources
 * count as chosen once they differ from the stored ones; until then they follow the (possibly new) language.
 */
internal fun UserPreferences.withGeneralSettings(settings: GeneralSettings): UserPreferences {
    val sources = settings.enabledEventSources.intersect(AppSettings.SELECTABLE_SOURCES.toSet())
    return copy(
        languageCode = settings.languageCode,
        themeMode = ThemeMode.valueOf(settings.theme.name),
        numerals = settings.numerals,
        calendars = settings.calendars.distinct().ifEmpty { calendars },
        weekStart = settings.weekStart,
        weekend = settings.weekend,
        islamicVariant = settings.islamicVariant,
        prayerMethod = settings.prayerMethod,
        asrJuristic = settings.asrJuristic,
        app =
            app.copy(
                dynamicColor = settings.dynamicColor,
                highContrast = settings.highContrast,
                boldText = settings.boldText,
                gradient = settings.gradient,
                showWeekNumbers = settings.showWeekNumbers,
                enabledEventSources = sources,
                eventSourcesChosen = app.eventSourcesChosen || sources != app.enabledEventSources,
                highLatitudeRule = settings.highLatitudeRule,
                subscriptionsNetworkAllowed = settings.subscriptionsNetworkAllowed,
                persistentNotification = settings.persistentNotification,
                persistentNotificationLargeNumber = settings.persistentNotificationLargeNumber,
                dynamicLauncherIcon = settings.dynamicLauncherIcon,
                rememberRecentSearches = settings.rememberRecentSearches,
                recentSearches = if (settings.rememberRecentSearches) app.recentSearches else emptyList(),
                allDayReminderMinute =
                    settings.allDayReminderMinute.takeIf { it in AppSettings.ALL_DAY_REMINDER_MINUTES }
                        ?: app.allDayReminderMinute,
            ),
    ).let { it.copy(app = it.app.withEventSourcesFor(it.languageCode)) }
}

/** Prayer conventions of these preferences: method, Asr and the high-latitude rule chosen in the settings (T-1500). */
internal fun UserPreferences.prayerSettings(): PrayerSettings =
    PrayerSettings(method = prayerMethod, asr = asrJuristic, highLatitude = app.highLatitudeRule)

/** The app theme (T-700) chosen in the settings (T-1500). */
internal fun UserPreferences.themeSettings(): ThemeSettings =
    ThemeSettings(
        mode = UiThemeMode.valueOf(themeMode.name),
        dynamicColor = app.dynamicColor,
        gradient = app.gradient,
        highContrast = app.highContrast,
        boldText = app.boldText,
    )

/**
 * [SubscriptionsStore] (T-1500) over the T-1003 subscriptions: addresses are normalized (webcal to https), a feed is
 * subscribed once, feeds download only while the network switch allows it, and the periodic refresh is rescheduled
 * after every change through [reschedule].
 */
internal class RoomSubscriptionsStore(
    private val dao: IcsSubscriptionDao,
    private val refresh: suspend (id: Long) -> RefreshOutcome,
    private val preferences: UserPreferencesRepository,
    private val policy: SubscriptionRefreshPolicy = SubscriptionRefreshPolicy(),
    private val reschedule: suspend () -> Unit,
) : SubscriptionsStore {
    override fun subscriptions(): Flow<List<SubscriptionItem>> =
        combine(dao.observeSubscriptions(), dao.observeCacheSummaries()) { list, summaries ->
            val byId = summaries.associateBy { it.subscriptionId }
            list.map { it.toItem(byId[it.id], policy) }
        }

    override suspend fun add(url: String): SubscriptionOutcome {
        val normalized = SubscriptionUrls.normalize(url)
        if (normalized !is SubscriptionUrl.Valid) return SubscriptionOutcome.INVALID_ADDRESS
        val existing = dao.observeSubscriptions().first()
        if (existing.any { SubscriptionUrls.normalize(it.url) == normalized }) {
            return SubscriptionOutcome.ALREADY_SUBSCRIBED
        }
        val name = runCatching { URI(normalized.url).host }.getOrNull() ?: normalized.url
        val subscription =
            IcsSubscriptionEntity(url = normalized.url, displayName = name, refreshIntervalMinutes = DAILY_MINUTES)
        val id = dao.insertSubscription(subscription)
        reschedule()
        return refresh(id)
    }

    override suspend fun remove(id: Long) {
        dao.deleteSubscription(id)
        reschedule()
    }

    override suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    ) {
        val subscription = dao.getSubscription(id) ?: return
        dao.updateSubscription(subscription.copy(enabled = enabled))
        reschedule()
    }

    override suspend fun refresh(id: Long): SubscriptionOutcome {
        val allowed =
            preferences.preferences
                .first()
                .app.subscriptionsNetworkAllowed
        if (!allowed) return SubscriptionOutcome.NETWORK_NOT_ALLOWED
        return when (val outcome = refresh.invoke(id)) {
            is RefreshOutcome.Failed -> outcome.error.toOutcome()
            else -> SubscriptionOutcome.DONE
        }
    }

    private companion object {
        /** New subscriptions refresh once a day (product choice). */
        const val DAILY_MINUTES = 1_440
    }
}

/** A stored subscription with its cache [summary] as the settings page's item, including its health (F03). */
internal fun IcsSubscriptionEntity.toItem(
    summary: IcsCacheSummary?,
    policy: SubscriptionRefreshPolicy = SubscriptionRefreshPolicy(),
): SubscriptionItem {
    val interval = policy.interval(this)
    return SubscriptionItem(
        id = id,
        name = displayName,
        url = url,
        enabled = enabled,
        lastFetchedAtEpochMillis = lastFetchedAtEpochMillis,
        health =
            SubscriptionHealthData(
                lastCheckedAtEpochMillis = lastCheckedAtEpochMillis,
                nextCheckAtEpochMillis = lastCheckedAtEpochMillis?.plus(interval.inWholeMilliseconds),
                refreshIntervalMinutes = interval.inWholeMinutes.toInt(),
                cachedEvents = summary?.eventCount ?: 0,
                cachedFromEpochMillis = summary?.firstStartEpochMillis,
                cachedUntilEpochMillis = summary?.lastEndEpochMillis,
                problemCount = problemCount,
                error = lastError?.let(::subscriptionError),
                httpStatus = lastError?.let(SubscriptionErrorCodes::httpStatus),
                errorAtEpochMillis = lastErrorAtEpochMillis,
            ),
    )
}

/** The failure kind of a stored [SubscriptionErrorCodes] code; unknown codes read as a network failure. */
internal fun subscriptionError(code: String): SubscriptionError {
    val status = SubscriptionErrorCodes.httpStatus(code)
    return when {
        status != null && (status >= SERVER_ERROR || status == TOO_MANY_REQUESTS) -> SubscriptionError.SERVER
        status != null -> SubscriptionError.NOT_AVAILABLE
        else -> STORED_ERRORS[code] ?: SubscriptionError.NETWORK
    }
}

private const val SERVER_ERROR = 500
private const val TOO_MANY_REQUESTS = 429

private val STORED_ERRORS: Map<String, SubscriptionError> =
    mapOf(
        SubscriptionErrorCodes.NETWORK to SubscriptionError.NETWORK,
        SubscriptionErrorCodes.TIMEOUT to SubscriptionError.TIMEOUT,
        SubscriptionErrorCodes.TOO_LARGE to SubscriptionError.TOO_LARGE,
        SubscriptionErrorCodes.INSECURE to SubscriptionError.INSECURE,
        SubscriptionErrorCodes.INVALID_ADDRESS to SubscriptionError.INVALID_ADDRESS,
        SubscriptionErrorCodes.UNREADABLE to SubscriptionError.UNREADABLE,
    )

/**
 * Reschedules the periodic subscription refresh (T-1003) from the stored subscriptions and the network switch; bound
 * after every subscription or settings change.
 */
internal fun subscriptionRescheduler(
    dao: IcsSubscriptionDao,
    preferences: UserPreferencesRepository,
    update: (List<IcsSubscriptionEntity>, networkAllowed: Boolean) -> Unit,
): suspend () -> Unit =
    {
        update(
            dao.observeSubscriptions().first(),
            preferences.preferences
                .first()
                .app.subscriptionsNetworkAllowed,
        )
    }

private fun RefreshError.toOutcome(): SubscriptionOutcome =
    when (this) {
        RefreshError.InsecureUrl, RefreshError.InvalidUrl -> SubscriptionOutcome.INVALID_ADDRESS
        else -> SubscriptionOutcome.FAILED
    }

/**
 * [RecentQueriesStore] (T-804) in the user preferences (T-1500): at most [AppSettings.MAX_RECENT_SEARCHES] queries,
 * newest first, a repeated query moves to the front, and nothing is kept while search history is turned off.
 */
internal class PreferencesRecentQueriesStore(
    private val preferences: UserPreferencesRepository,
) : RecentQueriesStore {
    override fun queries(): Flow<List<String>> =
        preferences.preferences
            .map {
                it.app.recentSearches
            }.distinctUntilChanged()

    override suspend fun add(query: String) {
        val text = query.trim()
        val key = SearchMatcher.key(text)
        if (key.isEmpty()) return
        preferences.update { current ->
            if (!current.app.rememberRecentSearches) return@update current
            val queries = listOf(text) + current.app.recentSearches.filterNot { SearchMatcher.key(it) == key }
            current.copy(app = current.app.copy(recentSearches = queries.take(AppSettings.MAX_RECENT_SEARCHES)))
        }
    }

    override suspend fun clear() {
        preferences.update { it.copy(app = it.app.copy(recentSearches = emptyList())) }
    }
}

/** [LevelCalibrationStore] (T-1303) in the user preferences (T-1500): one offset per orientation, within ±45°. */
internal class PreferencesLevelCalibrationStore(
    private val preferences: UserPreferencesRepository,
) : LevelCalibrationStore {
    override fun calibration(): Flow<LevelCalibration> =
        preferences.preferences.map { it.app.levelOffsets.toCalibration() }.distinctUntilChanged()

    override suspend fun save(calibration: LevelCalibration) {
        preferences.update { it.copy(app = it.app.copy(levelOffsets = calibration.toOffsets())) }
    }
}

/** Stored offsets keyed by orientation name; unknown names are ignored. Pitch is the tilt's x and roll its y. */
internal fun Map<String, LevelOffset>.toCalibration(): LevelCalibration =
    LevelCalibration(
        entries
            .mapNotNull { (name, offset) ->
                val orientation = DeviceOrientation.entries.firstOrNull { it.name == name }
                orientation?.let { it to Tilt(offset.pitchDegrees, offset.rollDegrees) }
            }.toMap()
            .toImmutableMap(),
    )

/** The calibration as stored offsets, clamped to the stored range. */
internal fun LevelCalibration.toOffsets(): Map<String, LevelOffset> =
    offsets.entries.associate { (orientation, tilt) ->
        val range = LevelOffset.OFFSET_RANGE
        orientation.name to LevelOffset(tilt.x.coerceIn(range), tilt.y.coerceIn(range))
    }
