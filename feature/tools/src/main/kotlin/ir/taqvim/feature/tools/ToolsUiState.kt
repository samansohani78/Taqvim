/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.CalendarSystem
import kotlinx.collections.immutable.ImmutableList

/** The tools of the Tools screen (T-1400), in tab order. */
enum class ToolsTab {
    CONVERTER,
    DISTANCE,
    DURATION,
    TIME_ZONES,
    QR,
}

/** What the user has typed into each tool. */
data class ToolsInputs(
    val converter: String = "",
    val distanceFrom: String = "",
    val distanceTo: String = "",
    val duration: String = "",
    val zoneQuery: String = "",
    val qr: String = "",
)

/** State of the Tools screen. */
data class ToolsUiState(
    val tab: ToolsTab = ToolsTab.CONVERTER,
    val inputs: ToolsInputs = ToolsInputs(),
    val content: ToolsContent = ToolsContent.Loading,
)

/** What the tools show. */
sealed interface ToolsContent {
    /** Preferences are loading. */
    data object Loading : ToolsContent

    data class Ready(
        val converter: ConverterResult,
        val distance: DistanceState,
        val duration: DurationState,
        val board: TimeZoneBoard,
        val qr: QrState,
    ) : ToolsContent
}

/** One date in one calendar, already formatted. */
data class ConvertedDate(
    val system: CalendarSystem,
    val long: String,
    val numeric: String,
    val iso: String,
)

sealed interface ConverterResult {
    /** The input contains no date. */
    data object NotRecognized : ConverterResult

    /** The date in every calendar; [isToday] when the input is blank. */
    data class Converted(
        val isToday: Boolean,
        val dates: ImmutableList<ConvertedDate>,
    ) : ConverterResult
}

/** One end of the distance tool. */
sealed interface DistanceEnd {
    data object Empty : DistanceEnd

    data object NotRecognized : DistanceEnd

    /** The recognized date in the primary calendar, long style. */
    data class Recognized(
        val date: String,
    ) : DistanceEnd
}

data class DistanceState(
    val from: DistanceEnd = DistanceEnd.Empty,
    val to: DistanceEnd = DistanceEnd.Empty,
    /** Present when both ends are recognized. */
    val result: DistanceResult? = null,
)

/** The distance between two dates, numbers in the language's digits. */
data class DistanceResult(
    /** Whether the second date is before the first. */
    val isBackward: Boolean,
    val days: String,
    /** "N days" in the language, or `null` without unit patterns (DT-009). */
    val daysText: String?,
    val weeks: String,
    val weekDays: String,
    /** The primary calendar, in which [years], [months] and [monthDays] are counted. */
    val calendar: CalendarSystem,
    val years: String,
    val months: String,
    val monthDays: String,
    val workdays: WorkdaysText,
)

/** Workdays from the earlier date up to (not including) the later one. */
sealed interface WorkdaysText {
    data object NotConfigured : WorkdaysText

    /** Longer than [DateTools.MAX_WORKDAY_SPAN_DAYS]. */
    data object TooLong : WorkdaysText

    data class Count(
        val value: String,
    ) : WorkdaysText
}

sealed interface DurationState {
    data object Empty : DurationState

    data class Invalid(
        val error: DurationError,
    ) : DurationState

    /** A value split into parts and totals, numbers in the language's digits. */
    data class Value(
        val isNegative: Boolean,
        /** Days, hours and minutes in the language, or `null` without unit patterns (DT-009). */
        val text: String?,
        val days: String,
        val hours: String,
        val minutes: String,
        val seconds: String,
        val totalHours: String,
        val totalMinutes: String,
    ) : DurationState
}

/** A row of the time-zone board. */
data class ZoneRow(
    val id: String,
    /** The last part of the IANA id, e.g. "New York". */
    val city: String,
    /** The platform's localized zone name. */
    val name: String,
    /** Local time, HH:mm in the language's digits. */
    val time: String,
    /** The local date minus the home date: −1, 0 or +1 (±2 across the date line). */
    val dayShift: Int,
    /** UTC offset such as `+03:30`. */
    val offset: String,
    val isHome: Boolean,
)

data class TimeZoneBoard(
    val rows: ImmutableList<ZoneRow>,
    /** Zone ids matching the search, not yet on the board. */
    val suggestions: ImmutableList<String>,
)

sealed interface QrState {
    data object Empty : QrState

    /** The text does not fit in a QR code. */
    data object TooLong : QrState

    data class Code(
        val matrix: QrMatrix,
    ) : QrState
}

/** User actions of the Tools screen. */
@Immutable
data class ToolsActions(
    val onSelectTab: (ToolsTab) -> Unit = {},
    val onInputsChange: (ToolsInputs) -> Unit = {},
    val onAddZone: (String) -> Unit = {},
    val onRemoveZone: (String) -> Unit = {},
    val onShareQr: () -> Unit = {},
)
