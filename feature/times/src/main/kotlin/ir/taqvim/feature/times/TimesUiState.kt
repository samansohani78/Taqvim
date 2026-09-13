/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import androidx.compose.runtime.Immutable
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlinx.collections.immutable.ImmutableList

/** State of the Times tab (T-1100). */
data class TimesUiState(
    val content: TimesContent = TimesContent.Loading,
    /** Whether sunset and midnight are shown besides the primary times. */
    val expanded: Boolean = false,
)

/** What the Times tab shows. */
sealed interface TimesContent {
    /** Preferences and place are loading. */
    data object Loading : TimesContent

    /** No place has been chosen yet. */
    data object NoLocation : TimesContent

    /** Times of the selected day. */
    data class Day(
        val placeName: String,
        /** The selected day in the settings' calendar, long style. */
        val dayTitle: String,
        val isToday: Boolean,
        /** Rows to show: the primary times, or every time when expanded; empty when [unavailable]. */
        val rows: ImmutableList<TimeRow>,
        /** Countdown to the next time; only for today. */
        val next: NextPrayerText?,
        val sunPath: SunPath?,
        /** Why the day has no times (polar day or night), or `null`. */
        val unavailable: PrayerTimesResult.Reason?,
    ) : TimesContent
}

/** A row of the times list; [time] is `null` where the time is undefined. */
data class TimeRow(
    val kind: PrayerKind,
    val time: String?,
    val isNext: Boolean,
)

/** The countdown to the next time, already localized. */
data class NextPrayerText(
    val kind: PrayerKind,
    val remaining: String,
    val progress: Float,
)

/** User actions of the Times tab. */
@Immutable
data class TimesActions(
    val onPreviousDay: () -> Unit = {},
    val onNextDay: () -> Unit = {},
    val onToday: () -> Unit = {},
    val onToggleExpanded: () -> Unit = {},
    val onPrintReport: () -> Unit = {},
)

/** The Sun's path between [sunrise] and [sunset]; [progress] is `null` for days other than today. */
data class SunPath(
    val progress: Float?,
    val sunrise: String,
    val sunset: String,
)
