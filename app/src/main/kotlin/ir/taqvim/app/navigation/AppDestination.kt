/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import ir.taqvim.app.R
import kotlinx.serialization.Serializable

/** A screen of the app (ADR-0015). Destinations are saved with the back stack, so each one is serializable. */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Calendar : AppDestination

    @Serializable
    data object Times : AppDestination

    @Serializable
    data object Tools : AppDestination

    @Serializable
    data object More : AppDestination

    @Serializable
    data object Year : AppDestination

    @Serializable
    data object Agenda : AppDestination

    @Serializable
    data object Astronomy : AppDestination

    @Serializable
    data object Compass : AppDestination

    @Serializable
    data object Level : AppDestination

    @Serializable
    data object Search : AppDestination

    @Serializable
    data object LocationSettings : AppDestination

    @Serializable
    data object AthanSettings : AppDestination

    @Serializable
    data object Subscriptions : AppDestination

    /** The settings home (T-1500), opened at the settings item named [initialItem] when one is given. */
    @Serializable
    data class Settings(
        val initialItem: String? = null,
    ) : AppDestination

    /** The timeline, showing the week of [initialDay] (a Julian day number) when one is given. */
    @Serializable
    data class Timeline(
        val initialDay: Long? = null,
    ) : AppDestination

    /**
     * The editor of the personal event [eventId], or of a new event when it is `null`: on [day] (a Julian day number)
     * from [startMinute] to [endMinute] when those are given (T-1103).
     */
    @Serializable
    data class EventEditor(
        val eventId: Long? = null,
        val day: Long? = null,
        val startMinute: Int? = null,
        val endMinute: Int? = null,
    ) : AppDestination

    /** The calendar opened on the Julian day [jdn] (links, year view, agenda and search; T-1103). */
    @Serializable
    data class Day(
        val jdn: Long,
    ) : AppDestination

    /** The Astronomy screen showing the planetary hours of the Julian day [day] (T-803, T-1103). */
    @Serializable
    data class PlanetaryHours(
        val day: Long,
    ) : AppDestination

    /** The tools opened on the date converter with [text] (T-1103 links and selected text). */
    @Serializable
    data class Converter(
        val text: String,
    ) : AppDestination

    /** Search opened with [query] (T-1103). */
    @Serializable
    data class SearchFor(
        val query: String,
    ) : AppDestination

    /** Backup and restore (T-1503). */
    @Serializable
    data object Backup : AppDestination

    /** The privacy dashboard (T-1503). */
    @Serializable
    data object Privacy : AppDestination

    /** About Taqvim: version, open-source licenses, data sources, diagnostics and problem reports (T-1504). */
    @Serializable
    data object About : AppDestination

    /** A screen the plan has but the app does not have yet. */
    @Serializable
    data class Pending(
        val feature: PendingFeature,
    ) : AppDestination
}

/** Screens that are planned but not built yet; they show a notice instead. */
@Serializable
enum class PendingFeature(
    @param:StringRes val notice: Int,
) {
    /** Shift work (T-803 menu). */
    SHIFT_WORK(R.string.pending_shift_work),

    /** A settings search entry without a screen of its own. */
    SETTINGS(R.string.pending_settings),

    /** Widget settings (T-1200). */
    WIDGETS(R.string.pending_widgets),
}

/** The destinations of the navigation bar or rail, in order. */
enum class TopLevelTab(
    val destination: AppDestination,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    CALENDAR(AppDestination.Calendar, R.string.nav_calendar, R.drawable.nav_ic_calendar),
    TIMES(AppDestination.Times, R.string.nav_times, R.drawable.nav_ic_times),
    TOOLS(AppDestination.Tools, R.string.nav_tools, R.drawable.nav_ic_tools),
    MORE(AppDestination.More, R.string.nav_more, R.drawable.nav_ic_more),
    ;

    companion object {
        /** The tab whose own screen is [destination], or `null` for other screens. */
        fun of(destination: AppDestination): TopLevelTab? = entries.firstOrNull { it.destination == destination }
    }
}
