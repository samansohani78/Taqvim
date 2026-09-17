/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes

/** How a settings item reads and changes [GeneralSettings]. */
sealed interface SettingsControl {
    /** A switch; when [warning] is set, turning it on is confirmed in a dialog showing that text first. */
    data class Toggle(
        val read: (GeneralSettings) -> Boolean,
        val write: (GeneralSettings, Boolean) -> GeneralSettings,
        @param:StringRes val warning: Int? = null,
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
