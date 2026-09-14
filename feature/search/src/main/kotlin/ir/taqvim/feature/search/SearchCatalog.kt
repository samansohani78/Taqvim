/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import android.content.res.Resources
import androidx.annotation.StringRes

/** A settings page search can open; [title] and [keywords] (`|`-separated synonyms) are string resources. */
enum class SettingsEntry(
    @param:StringRes val title: Int,
    @param:StringRes val keywords: Int,
) {
    LANGUAGE(R.string.search_settings_language, R.string.search_settings_language_keywords),
    LOCATION(R.string.search_settings_location, R.string.search_settings_location_keywords),
    CALENDARS(R.string.search_settings_calendars, R.string.search_settings_calendars_keywords),
    PRAYER_TIMES(R.string.search_settings_prayer_times, R.string.search_settings_prayer_times_keywords),
    ATHAN(R.string.search_settings_athan, R.string.search_settings_athan_keywords),
    NOTIFICATIONS(R.string.search_settings_notifications, R.string.search_settings_notifications_keywords),
    THEME(R.string.search_settings_theme, R.string.search_settings_theme_keywords),
    WIDGETS(R.string.search_settings_widgets, R.string.search_settings_widgets_keywords),
    BACKUP(R.string.search_settings_backup, R.string.search_settings_backup_keywords),
    PRIVACY(R.string.search_settings_privacy, R.string.search_settings_privacy_keywords),
    ABOUT(R.string.search_settings_about, R.string.search_settings_about_keywords),
}

/** A tool or screen search can open; [title] and [keywords] (`|`-separated synonyms) are string resources. */
enum class ToolEntry(
    @param:StringRes val title: Int,
    @param:StringRes val keywords: Int,
) {
    CONVERTER(R.string.search_tool_converter, R.string.search_tool_converter_keywords),
    DATE_DISTANCE(R.string.search_tool_distance, R.string.search_tool_distance_keywords),
    DURATION(R.string.search_tool_duration, R.string.search_tool_duration_keywords),
    TIME_ZONES(R.string.search_tool_time_zones, R.string.search_tool_time_zones_keywords),
    QR_CODE(R.string.search_tool_qr, R.string.search_tool_qr_keywords),
    COMPASS(R.string.search_tool_compass, R.string.search_tool_compass_keywords),
    LEVEL(R.string.search_tool_level, R.string.search_tool_level_keywords),
    ASTRONOMY(R.string.search_tool_astronomy, R.string.search_tool_astronomy_keywords),
    PRAYER_TIMES(R.string.search_tool_prayer_times, R.string.search_tool_prayer_times_keywords),
    AGENDA(R.string.search_tool_agenda, R.string.search_tool_agenda_keywords),
    YEAR(R.string.search_tool_year, R.string.search_tool_year_keywords),
}

/** A setting or tool offered to the search: where it leads, its title and the synonyms it is also found by. */
data class SearchEntry(
    val target: SearchTarget,
    val title: String,
    val keywords: List<String>,
)

/** Every [SettingsEntry] and [ToolEntry] titled from [resources] in the current app language. */
class ResourceSearchCatalog(
    private val resources: Resources,
) : SearchCatalogSource {
    override fun entries(): List<SearchEntry> =
        SettingsEntry.entries.map { entry(SearchTarget.Settings(it), it.title, it.keywords) } +
            ToolEntry.entries.map { entry(SearchTarget.Tool(it), it.title, it.keywords) }

    private fun entry(
        target: SearchTarget,
        @StringRes title: Int,
        @StringRes keywords: Int,
    ) = SearchEntry(target, resources.getString(title), splitKeywords(resources.getString(keywords)))

    companion object {
        /** The synonyms of a `|`-separated keyword resource, trimmed, blanks dropped. */
        fun splitKeywords(text: String): List<String> = text.split('|').map(String::trim).filter(String::isNotEmpty)
    }
}
