/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.HighLatitudeRule
import kotlinx.coroutines.flow.Flow

/** The light or dark appearance chosen in the settings. */
enum class ThemeChoice {
    SYSTEM,
    LIGHT,
    DARK,
    BLACK,
}

/** The settings the settings screens (T-1500) edit, as stored by [GeneralSettingsStore]. */
data class GeneralSettings(
    val languageCode: String,
    val theme: ThemeChoice,
    val dynamicColor: Boolean,
    val highContrast: Boolean,
    val boldText: Boolean,
    val gradient: Boolean,
    val numerals: NumeralSystem,
    /** Calendars in priority order: the first is the main calendar and the second the secondary one. */
    val calendars: List<CalendarSystem>,
    val weekStart: Weekday,
    val weekend: Set<Weekday>,
    val islamicVariant: IslamicVariant,
    val showWeekNumbers: Boolean,
    /** Dataset sources shown in the calendar; personal events are always shown. */
    val enabledEventSources: Set<EventSource>,
    val prayerMethod: PrayerMethod,
    val asrJuristic: AsrJuristic,
    val highLatitudeRule: HighLatitudeRule,
    val subscriptionsNetworkAllowed: Boolean,
    val persistentNotification: Boolean,
    val rememberRecentSearches: Boolean,
    /** Whether any recent search is stored, so clearing them does something. */
    val hasRecentSearches: Boolean,
    /** Minute of the day (0‥1439) at which reminders of all-day events sound (T-1001, T-1002). */
    val allDayReminderMinute: Int = DEFAULT_ALL_DAY_REMINDER_MINUTE,
    /** Whether the persistent notification (T-1213) shows the day number as a large icon. */
    val persistentNotificationLargeNumber: Boolean = false,
    /** Whether the launcher icon shows today's day number (T-1214); turning it on asks for confirmation. */
    val dynamicLauncherIcon: Boolean = false,
) {
    companion object {
        /** 09:00. */
        const val DEFAULT_ALL_DAY_REMINDER_MINUTE: Int = 540
    }
}

/** What the settings screens show: the app language and the stored settings. */
data class GeneralSettingsData(
    val language: LanguageSpec,
    val settings: GeneralSettings,
)

/** Reads and stores the settings of the settings screens; bound in `:app` over the user preferences. */
interface GeneralSettingsStore {
    fun settings(): Flow<GeneralSettingsData>

    /** Atomically replaces the settings with [transform] of the current value. */
    suspend fun update(transform: (GeneralSettings) -> GeneralSettings)

    suspend fun clearRecentSearches()
}

/** A calendar subscription (T-1003) as listed in the settings. */
data class SubscriptionItem(
    val id: Long,
    val name: String,
    val url: String,
    val enabled: Boolean,
    /** When the feed was last downloaded, in epoch milliseconds, or `null` when never. */
    val lastFetchedAtEpochMillis: Long?,
    /** What the subscription page shows about the feed's state (F03). */
    val health: SubscriptionHealthData = SubscriptionHealthData(),
)

/** Why the last refresh of a subscription failed (F03). */
enum class SubscriptionError {
    NETWORK,
    TIMEOUT,

    /** The server answered with an error of its own (HTTP 5xx or 429). */
    SERVER,

    /** The server has no calendar at the address (any other HTTP error). */
    NOT_AVAILABLE,
    TOO_LARGE,
    INSECURE,
    INVALID_ADDRESS,
    UNREADABLE,
}

/**
 * The refresh state of a subscription (F03); times are epoch milliseconds. [refreshIntervalMinutes] is how often the
 * feed is due, [nextCheckAtEpochMillis] when it is next due (`null` while paused), and [problemCount] how many items of
 * the last downloaded feed were ignored or approximated.
 */
data class SubscriptionHealthData(
    val lastCheckedAtEpochMillis: Long? = null,
    val nextCheckAtEpochMillis: Long? = null,
    val refreshIntervalMinutes: Int = 0,
    val cachedEvents: Int = 0,
    val cachedFromEpochMillis: Long? = null,
    val cachedUntilEpochMillis: Long? = null,
    val problemCount: Int = 0,
    val error: SubscriptionError? = null,
    val httpStatus: Int? = null,
    val errorAtEpochMillis: Long? = null,
)

/** The outcome of adding or refreshing a subscription. */
enum class SubscriptionOutcome {
    DONE,
    INVALID_ADDRESS,
    ALREADY_SUBSCRIBED,
    NETWORK_NOT_ALLOWED,
    FAILED,
}

/** Lists and changes calendar subscriptions; bound in `:app` over the T-1003 subscription code. */
interface SubscriptionsStore {
    fun subscriptions(): Flow<List<SubscriptionItem>>

    /** Subscribes to the feed at [url] (`https://` or `webcal://`). */
    suspend fun add(url: String): SubscriptionOutcome

    suspend fun remove(id: Long)

    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    )

    /** Downloads the feed now. */
    suspend fun refresh(id: Long): SubscriptionOutcome
}
