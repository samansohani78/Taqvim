/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.data.preferences.proto.AppSettingsProto
import ir.taqvim.data.preferences.proto.EventSourceProto
import ir.taqvim.data.preferences.proto.HighLatitudeRuleProto
import ir.taqvim.data.preferences.proto.LevelOffsetProto

/** Bubble level calibration of one device orientation (T-1303), in degrees. */
data class LevelOffset(
    val pitchDegrees: Double,
    val rollDegrees: Double,
) {
    init {
        require(pitchDegrees.isFinite() && rollDegrees.isFinite()) { "level offsets must be finite" }
        require(pitchDegrees in OFFSET_RANGE && rollDegrees in OFFSET_RANGE) { "level offsets must be within ±45°" }
    }

    companion object {
        /** Allowed calibration offset, in degrees. */
        val OFFSET_RANGE: ClosedFloatingPointRange<Double> = -45.0..45.0
    }
}

/**
 * Settings of the settings screens (T-1500) that are not part of the older preference fields: appearance extras,
 * calendar display, event sources, the high-latitude rule, notifications, search history and tool state.
 */
data class AppSettings(
    val dynamicColor: Boolean,
    val highContrast: Boolean,
    val boldText: Boolean,
    val gradient: Boolean,
    val showWeekNumbers: Boolean,
    /** Dataset sources shown in the calendar; personal events ([EventSource.USER]) are always shown. */
    val enabledEventSources: Set<EventSource>,
    val highLatitudeRule: HighLatitudeRule,
    /** Whether calendar subscriptions (T-1003) may refresh over the network. */
    val subscriptionsNetworkAllowed: Boolean,
    /** Whether the persistent date notification (T-1213) is shown. */
    val persistentNotification: Boolean,
    val rememberRecentSearches: Boolean,
    /** Recent search queries, newest first (T-804); kept on this device only. */
    val recentSearches: List<String>,
    /** IANA zone ids on the time zone board (T-1400), in display order. */
    val timeZoneBoard: List<String>,
    /** Level calibration per orientation name (T-1303); kept on this device only. */
    val levelOffsets: Map<String, LevelOffset>,
    /** When reminders of all-day events and official events sound (T-1001, T-1002), in minutes after midnight. */
    val allDayReminderMinute: Int = DEFAULT_ALL_DAY_REMINDER_MINUTE,
    /**
     * Whether the user chose [enabledEventSources] in the settings. Until then the sources follow the language
     * default ([defaultEventSources], ADR-0007 §3 addendum) and change with the language.
     */
    val eventSourcesChosen: Boolean = false,
    /** Whether the persistent date notification (T-1213) shows the day number as a large icon. */
    val persistentNotificationLargeNumber: Boolean = false,
    /** Whether the launcher icon shows today's day number (T-1214, ADR-0022); off by default. */
    val dynamicLauncherIcon: Boolean = false,
) {
    init {
        require(allDayReminderMinute in ALL_DAY_REMINDER_MINUTES) { "all-day reminder time must be within 0..1439" }
        require(EventSource.USER !in enabledEventSources) { "personal events are not a selectable source" }
        require(recentSearches.size <= MAX_RECENT_SEARCHES) { "at most $MAX_RECENT_SEARCHES recent searches" }
        require(timeZoneBoard.size <= MAX_BOARD_ZONES) { "at most $MAX_BOARD_ZONES board zones" }
    }

    companion object {
        const val MAX_RECENT_SEARCHES: Int = 10
        const val MAX_BOARD_ZONES: Int = 24

        /** 09:00, the planner's default all-day reminder time (product choice). */
        const val DEFAULT_ALL_DAY_REMINDER_MINUTE: Int = 540

        /** Valid all-day reminder times: every minute of the day. */
        val ALL_DAY_REMINDER_MINUTES: IntRange = 0..1439

        /** Dataset sources a user can turn on or off. */
        val SELECTABLE_SOURCES: List<EventSource> = EventSource.entries - EventSource.USER

        /** The national official source shown by default to each language's users (ADR-0007 §3 addendum). */
        private val NATIONAL_SOURCES: Map<String, EventSource> =
            mapOf(
                "fa" to EventSource.IRAN_OFFICIAL,
                "prs" to EventSource.AFGHANISTAN_OFFICIAL,
                "ps" to EventSource.AFGHANISTAN_OFFICIAL,
                "ne" to EventSource.NEPAL_OFFICIAL,
            )

        /**
         * Dataset sources shown by default to [languageCode]'s users: the international days, plus the national
         * official holidays of `fa` (Iran), `prs`/`ps` (Afghanistan) and `ne` (Nepal). Ancient Iranian festivals are
         * off for everyone (docs/PLAN.md §5.1).
         */
        fun defaultEventSources(languageCode: String): Set<EventSource> =
            setOfNotNull(EventSource.INTERNATIONAL, NATIONAL_SOURCES[languageCode])

        /** First-run app settings of [languageCode]: [DEFAULT] with that language's [defaultEventSources]. */
        fun defaultsFor(languageCode: String): AppSettings =
            DEFAULT.copy(enabledEventSources = defaultEventSources(languageCode))

        /**
         * Product defaults apart from the language-dependent event sources ([defaultsFor]): dynamic color on, only
         * the international days, the prayer library's angle-based high-latitude rule, subscriptions not allowed to
         * use the network until the user turns it on (docs/PLAN.md T-1804), no persistent notification and search
         * history remembered.
         */
        val DEFAULT: AppSettings =
            AppSettings(
                dynamicColor = true,
                highContrast = false,
                boldText = false,
                gradient = false,
                showWeekNumbers = false,
                enabledEventSources = setOf(EventSource.INTERNATIONAL),
                highLatitudeRule = HighLatitudeRule.ANGLE_BASED,
                subscriptionsNetworkAllowed = false,
                persistentNotification = false,
                rememberRecentSearches = true,
                recentSearches = emptyList(),
                timeZoneBoard = emptyList(),
                levelOffsets = emptyMap(),
            )
    }
}

/** These settings with the event sources of [languageCode] unless the user chose them. */
fun AppSettings.withEventSourcesFor(languageCode: String): AppSettings =
    if (eventSourcesChosen) this else copy(enabledEventSources = AppSettings.defaultEventSources(languageCode))

private const val SOURCE = "EVENT_SOURCE_"
private const val RULE = "HIGH_LATITUDE_RULE_"

/** Trimmed, non-blank, distinct values in order, at most [limit]. */
private fun List<String>.cleaned(limit: Int): List<String> =
    map(String::trim).filter(String::isNotEmpty).distinct().take(limit)

/**
 * The stored app settings. Unknown enum values are dropped (an unknown rule reads as the default); lists are
 * trimmed, de-duplicated and capped; unusable level offsets are ignored and out-of-range ones clamped.
 */
internal fun AppSettingsProto.toDomain(): AppSettings =
    AppSettings(
        dynamicColor = dynamicColor,
        highContrast = highContrast,
        boldText = boldText,
        gradient = gradient,
        showWeekNumbers = showWeekNumbers,
        enabledEventSources =
            enabledEventSourcesList
                .mapNotNull { stored -> AppSettings.SELECTABLE_SOURCES.firstOrNull { SOURCE + it.name == stored.name } }
                .toSet(),
        highLatitudeRule =
            HighLatitudeRule.entries.firstOrNull { RULE + it.name == highLatitudeRule.name }
                ?: AppSettings.DEFAULT.highLatitudeRule,
        subscriptionsNetworkAllowed = subscriptionsNetworkAllowed,
        persistentNotification = persistentNotification,
        rememberRecentSearches = rememberRecentSearches,
        recentSearches = recentSearchesList.cleaned(AppSettings.MAX_RECENT_SEARCHES),
        timeZoneBoard = timeZoneBoardList.cleaned(AppSettings.MAX_BOARD_ZONES),
        levelOffsets =
            levelOffsetsList
                .filter { it.orientation.isNotBlank() && it.pitchDegrees.isFinite() && it.rollDegrees.isFinite() }
                .associate { stored ->
                    stored.orientation.trim() to
                        LevelOffset(
                            stored.pitchDegrees.coerceIn(LevelOffset.OFFSET_RANGE),
                            stored.rollDegrees.coerceIn(LevelOffset.OFFSET_RANGE),
                        )
                },
        allDayReminderMinute =
            allDayReminderMinute.takeIf { hasAllDayReminderMinute() && it in AppSettings.ALL_DAY_REMINDER_MINUTES }
                ?: AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE,
        eventSourcesChosen = eventSourcesChosen,
        persistentNotificationLargeNumber = persistentNotificationLargeNumber,
        dynamicLauncherIcon = dynamicLauncherIcon,
    )

internal fun AppSettings.toProto(): AppSettingsProto =
    AppSettingsProto
        .newBuilder()
        .setDynamicColor(dynamicColor)
        .setHighContrast(highContrast)
        .setBoldText(boldText)
        .setGradient(gradient)
        .setShowWeekNumbers(showWeekNumbers)
        .addAllEnabledEventSources(
            AppSettings.SELECTABLE_SOURCES.filter { it in enabledEventSources }.map {
                EventSourceProto.valueOf(SOURCE + it.name)
            },
        ).setHighLatitudeRule(HighLatitudeRuleProto.valueOf(RULE + highLatitudeRule.name))
        .setSubscriptionsNetworkAllowed(subscriptionsNetworkAllowed)
        .setPersistentNotification(persistentNotification)
        .setRememberRecentSearches(rememberRecentSearches)
        .addAllRecentSearches(recentSearches)
        .addAllTimeZoneBoard(timeZoneBoard)
        .addAllLevelOffsets(
            levelOffsets.entries.sortedBy { it.key }.map { (orientation, offset) ->
                LevelOffsetProto
                    .newBuilder()
                    .setOrientation(orientation)
                    .setPitchDegrees(offset.pitchDegrees)
                    .setRollDegrees(offset.rollDegrees)
                    .build()
            },
        ).setAllDayReminderMinute(allDayReminderMinute)
        .setEventSourcesChosen(eventSourcesChosen)
        .setPersistentNotificationLargeNumber(persistentNotificationLargeNumber)
        .setDynamicLauncherIcon(dynamicLauncherIcon)
        .build()
