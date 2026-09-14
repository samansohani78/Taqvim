/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem

/** The three tabs of the settings home (docs/PLAN.md T-1500). */
enum class SettingsTab(
    @param:StringRes val title: Int,
) {
    INTERFACE_CALENDAR(R.string.settings_tab_interface_calendar),
    WIDGETS_NOTIFICATION(R.string.settings_tab_widgets_notification),
    LOCATION_ATHAN(R.string.settings_tab_location_athan),
}

/** Pages opened from the settings home. */
enum class SettingsDestination {
    LOCATION,
    ATHAN,
    SUBSCRIPTIONS,
    WIDGETS,
}

/** One option of a choice: a resource [label], or a ready [text] (e.g. a language's own name). */
data class SettingsOption(
    val key: String,
    @param:StringRes val label: Int = 0,
    val text: String? = null,
)

/** How a settings item reads and changes [GeneralSettings]. */
sealed interface SettingsControl {
    data class Toggle(
        val read: (GeneralSettings) -> Boolean,
        val write: (GeneralSettings, Boolean) -> GeneralSettings,
    ) : SettingsControl

    /** One of [options]; [read] and [write] use option keys. */
    data class Choice(
        val options: List<SettingsOption>,
        val read: (GeneralSettings) -> String,
        val write: (GeneralSettings, String) -> GeneralSettings,
    ) : SettingsControl

    /** Any set of [options] (at least one unless [allowEmpty]). */
    data class MultiChoice(
        val options: List<SettingsOption>,
        val allowEmpty: Boolean,
        val read: (GeneralSettings) -> Set<String>,
        val write: (GeneralSettings, Set<String>) -> GeneralSettings,
    ) : SettingsControl

    data class Link(
        val destination: SettingsDestination,
    ) : SettingsControl

    data object ClearRecentSearches : SettingsControl

    /** A time of day chosen every [stepMinutes] minutes; [read] and [write] use minutes of the day. */
    data class TimeOfDay(
        val stepMinutes: Int,
        val read: (GeneralSettings) -> Int,
        val write: (GeneralSettings, Int) -> GeneralSettings,
    ) : SettingsControl
}

/** Every item of the settings home, in display order; [keywords] are `|`-separated synonyms for settings search. */
enum class SettingsItemId(
    val tab: SettingsTab,
    @param:StringRes val title: Int,
    @param:StringRes val keywords: Int,
) {
    LANGUAGE(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_language, R.string.settings_keywords_language),
    THEME(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_theme, R.string.settings_keywords_theme),
    DYNAMIC_COLOR(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_dynamic_color,
        R.string.settings_keywords_theme,
    ),
    HIGH_CONTRAST(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_high_contrast,
        R.string.settings_keywords_theme,
    ),
    BOLD_TEXT(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_bold_text, R.string.settings_keywords_theme),
    GRADIENT(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_gradient, R.string.settings_keywords_theme),
    NUMERALS(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_numerals, R.string.settings_keywords_numerals),
    MAIN_CALENDAR(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_main_calendar,
        R.string.settings_keywords_calendar,
    ),
    SECONDARY_CALENDAR(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_secondary_calendar,
        R.string.settings_keywords_calendar,
    ),
    WEEK_START(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_week_start, R.string.settings_keywords_week),
    WEEKEND(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_weekend, R.string.settings_keywords_week),
    ISLAMIC_VARIANT(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_islamic_variant,
        R.string.settings_keywords_hijri,
    ),
    WEEK_NUMBERS(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_week_numbers, R.string.settings_keywords_week),
    EVENT_SOURCES(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_event_sources,
        R.string.settings_keywords_events,
    ),
    SUBSCRIPTIONS(SettingsTab.INTERFACE_CALENDAR, R.string.settings_item_subscriptions, R.string.settings_keywords_ics),
    SUBSCRIPTIONS_NETWORK(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_subscriptions_network,
        R.string.settings_keywords_ics,
    ),
    REMEMBER_SEARCHES(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_remember_searches,
        R.string.settings_keywords_search,
    ),
    CLEAR_SEARCHES(
        SettingsTab.INTERFACE_CALENDAR,
        R.string.settings_item_clear_searches,
        R.string.settings_keywords_search,
    ),
    WIDGETS(SettingsTab.WIDGETS_NOTIFICATION, R.string.settings_item_widgets, R.string.settings_keywords_widgets),
    PERSISTENT_NOTIFICATION(
        SettingsTab.WIDGETS_NOTIFICATION,
        R.string.settings_item_persistent_notification,
        R.string.settings_keywords_notification,
    ),
    ALL_DAY_REMINDER_TIME(
        SettingsTab.WIDGETS_NOTIFICATION,
        R.string.settings_item_all_day_reminder_time,
        R.string.settings_keywords_reminder,
    ),
    LOCATION(SettingsTab.LOCATION_ATHAN, R.string.settings_item_location, R.string.settings_keywords_location),
    ATHAN(SettingsTab.LOCATION_ATHAN, R.string.settings_item_athan, R.string.settings_keywords_athan),
    PRAYER_METHOD(SettingsTab.LOCATION_ATHAN, R.string.settings_item_prayer_method, R.string.settings_keywords_prayer),
    ASR_JURISTIC(SettingsTab.LOCATION_ATHAN, R.string.settings_item_asr, R.string.settings_keywords_prayer),
    HIGH_LATITUDE(SettingsTab.LOCATION_ATHAN, R.string.settings_item_high_latitude, R.string.settings_keywords_prayer),
}

/** The control of every [SettingsItemId]: the table the settings home and its tests are driven by. */
internal object SettingsCatalog {
    /** Key of "no secondary calendar". */
    const val NO_CALENDAR: String = "NONE"

    /** Times of day offered for all-day reminders are this many minutes apart. */
    const val REMINDER_TIME_STEP_MINUTES: Int = 30

    /** Calendars that can be chosen; the Nepali calendar becomes available with T-105. */
    val CALENDARS: List<CalendarSystem> =
        listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)

    fun control(id: SettingsItemId): SettingsControl =
        when (id.tab) {
            SettingsTab.INTERFACE_CALENDAR -> interfaceControl(id)
            SettingsTab.WIDGETS_NOTIFICATION, SettingsTab.LOCATION_ATHAN -> otherControl(id)
        }

    private fun interfaceControl(id: SettingsItemId): SettingsControl =
        appearanceControl(id)
            ?: calendarControl(id)
            ?: requireNotNull(eventAndSearchControl(id)) { "no control for $id" }

    private fun appearanceControl(id: SettingsItemId): SettingsControl? =
        when (id) {
            SettingsItemId.LANGUAGE -> {
                SettingsControl.Choice(
                    options = LanguageTable.languages.map { SettingsOption(it.code, text = it.nativeName) },
                    read = { it.languageCode },
                    write = { settings, key -> settings.copy(languageCode = key) },
                )
            }

            SettingsItemId.THEME -> {
                enumChoice(SettingsLabels.themes, { it.theme }) { s, v -> s.copy(theme = v) }
            }

            SettingsItemId.DYNAMIC_COLOR -> {
                toggle({ it.dynamicColor }) { s, v -> s.copy(dynamicColor = v) }
            }

            SettingsItemId.HIGH_CONTRAST -> {
                toggle({ it.highContrast }) { s, v -> s.copy(highContrast = v) }
            }

            SettingsItemId.BOLD_TEXT -> {
                toggle({ it.boldText }) { s, v -> s.copy(boldText = v) }
            }

            SettingsItemId.GRADIENT -> {
                toggle({ it.gradient }) { s, v -> s.copy(gradient = v) }
            }

            SettingsItemId.NUMERALS -> {
                enumChoice(SettingsLabels.numerals, { it.numerals }) { s, v -> s.copy(numerals = v) }
            }

            else -> {
                null
            }
        }

    private fun calendarControl(id: SettingsItemId): SettingsControl? =
        when (id) {
            SettingsItemId.MAIN_CALENDAR -> {
                SettingsControl.Choice(
                    options = CALENDARS.map { SettingsOption(it.name, SettingsLabels.calendars.getValue(it)) },
                    read = { it.calendars.first().name },
                    write = { settings, key -> settings.withMainCalendar(CalendarSystem.valueOf(key)) },
                )
            }

            SettingsItemId.SECONDARY_CALENDAR -> {
                SettingsControl.Choice(
                    options =
                        listOf(SettingsOption(NO_CALENDAR, R.string.settings_calendar_none)) +
                            CALENDARS.map { SettingsOption(it.name, SettingsLabels.calendars.getValue(it)) },
                    read = { it.calendars.getOrNull(1)?.name ?: NO_CALENDAR },
                    write = { settings, key -> settings.withSecondaryCalendar(key) },
                )
            }

            SettingsItemId.WEEK_START -> {
                enumChoice(SettingsLabels.weekdays, { it.weekStart }) { s, v -> s.copy(weekStart = v) }
            }

            SettingsItemId.WEEKEND -> {
                enumMultiChoice(SettingsLabels.weekdays, allowEmpty = false, { it.weekend }) { s, v ->
                    s.copy(weekend = v)
                }
            }

            SettingsItemId.ISLAMIC_VARIANT -> {
                enumChoice(SettingsLabels.islamicVariants, { it.islamicVariant }) { s, v -> s.copy(islamicVariant = v) }
            }

            SettingsItemId.WEEK_NUMBERS -> {
                toggle({ it.showWeekNumbers }) { s, v -> s.copy(showWeekNumbers = v) }
            }

            else -> {
                null
            }
        }

    private fun eventAndSearchControl(id: SettingsItemId): SettingsControl? =
        when (id) {
            SettingsItemId.EVENT_SOURCES -> {
                enumMultiChoice(SettingsLabels.eventSources, allowEmpty = true, { it.enabledEventSources }) { s, v ->
                    s.copy(enabledEventSources = v)
                }
            }

            SettingsItemId.SUBSCRIPTIONS -> {
                SettingsControl.Link(SettingsDestination.SUBSCRIPTIONS)
            }

            SettingsItemId.SUBSCRIPTIONS_NETWORK -> {
                toggle({ it.subscriptionsNetworkAllowed }) { s, v -> s.copy(subscriptionsNetworkAllowed = v) }
            }

            SettingsItemId.REMEMBER_SEARCHES -> {
                toggle({ it.rememberRecentSearches }) { s, v -> s.copy(rememberRecentSearches = v) }
            }

            SettingsItemId.CLEAR_SEARCHES -> {
                SettingsControl.ClearRecentSearches
            }

            else -> {
                null
            }
        }

    private fun otherControl(id: SettingsItemId): SettingsControl =
        when (id) {
            SettingsItemId.WIDGETS -> {
                SettingsControl.Link(SettingsDestination.WIDGETS)
            }

            SettingsItemId.PERSISTENT_NOTIFICATION -> {
                toggle({ it.persistentNotification }) { s, v -> s.copy(persistentNotification = v) }
            }

            SettingsItemId.ALL_DAY_REMINDER_TIME -> {
                SettingsControl.TimeOfDay(REMINDER_TIME_STEP_MINUTES, { it.allDayReminderMinute }) { s, v ->
                    s.copy(allDayReminderMinute = v)
                }
            }

            SettingsItemId.LOCATION -> {
                SettingsControl.Link(SettingsDestination.LOCATION)
            }

            SettingsItemId.ATHAN -> {
                SettingsControl.Link(SettingsDestination.ATHAN)
            }

            SettingsItemId.PRAYER_METHOD -> {
                enumChoice(SettingsLabels.prayerMethods, { it.prayerMethod }) { s, v -> s.copy(prayerMethod = v) }
            }

            SettingsItemId.ASR_JURISTIC -> {
                enumChoice(SettingsLabels.asrJuristics, { it.asrJuristic }) { s, v -> s.copy(asrJuristic = v) }
            }

            SettingsItemId.HIGH_LATITUDE -> {
                enumChoice(SettingsLabels.highLatitudeRules, { it.highLatitudeRule }) { s, v ->
                    s.copy(highLatitudeRule = v)
                }
            }

            else -> {
                error("no control for $id")
            }
        }

    private fun toggle(
        read: (GeneralSettings) -> Boolean,
        write: (GeneralSettings, Boolean) -> GeneralSettings,
    ) = SettingsControl.Toggle(read, write)

    private inline fun <reified E : Enum<E>> enumChoice(
        labels: Map<E, Int>,
        crossinline read: (GeneralSettings) -> E,
        crossinline write: (GeneralSettings, E) -> GeneralSettings,
    ) = SettingsControl.Choice(
        options = labels.map { (value, label) -> SettingsOption(value.name, label) },
        read = { read(it).name },
        write = { settings, key -> write(settings, enumValueOf(key)) },
    )

    private inline fun <reified E : Enum<E>> enumMultiChoice(
        labels: Map<E, Int>,
        allowEmpty: Boolean,
        crossinline read: (GeneralSettings) -> Set<E>,
        crossinline write: (GeneralSettings, Set<E>) -> GeneralSettings,
    ) = SettingsControl.MultiChoice(
        options = labels.map { (value, label) -> SettingsOption(value.name, label) },
        allowEmpty = allowEmpty,
        read = { settings -> read(settings).mapTo(mutableSetOf()) { it.name } },
        write = { settings, keys -> write(settings, keys.mapTo(mutableSetOf()) { enumValueOf<E>(it) }) },
    )

    /** [calendar] first, keeping the others in order. */
    private fun GeneralSettings.withMainCalendar(calendar: CalendarSystem): GeneralSettings =
        copy(calendars = listOf(calendar) + (calendars - calendar))

    /** The main calendar, then [key]'s calendar (none for [NO_CALENDAR]), then the rest in order. */
    private fun GeneralSettings.withSecondaryCalendar(key: String): GeneralSettings {
        val main = calendars.first()
        if (key == NO_CALENDAR) return copy(calendars = listOf(main))
        val secondary = CalendarSystem.valueOf(key)
        if (secondary == main) return this
        return copy(calendars = listOf(main, secondary) + (calendars - main - secondary))
    }
}
