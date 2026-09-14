/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlinx.collections.immutable.ImmutableList

/** State of the year view (T-805). */
data class YearUiState(
    /** `null` while today and the preferences are loading. */
    val content: YearContent? = null,
)

/** The loaded year view. */
data class YearContent(
    val today: Jdn,
    /** Available calendars in the user's order; the calendar pager has one page per calendar. */
    val calendars: ImmutableList<CalendarSystem>,
    /** Index in [calendars] of the calendar shown. */
    val calendarIndex: Int,
    /** A day of the shown year; every calendar page shows its own year that contains this day. */
    val anchorDay: Jdn,
    /** The shown year in the calendar [calendarIndex]. */
    val year: Int,
    /** Mini months per row: the zoom level, from [YearZoom.MIN_COLUMNS] to [YearZoom.MAX_COLUMNS]. */
    val columns: Int,
    /** Whether the year selection grid replaces the months of the shown calendar. */
    val isPickingYear: Boolean,
    val weekStart: Weekday,
    val islamicVariant: IslamicVariant,
    /** App language code for month names, digits and spoken summaries. */
    val languageCode: String,
    /** Holiday and weekend flags of the days of [year]; `null` while they load. */
    val days: ImmutableList<YearDay>?,
)

/** Zoom levels of the year grid, as mini months per row. */
object YearZoom {
    const val MIN_COLUMNS: Int = 1
    const val MAX_COLUMNS: Int = 4
    const val DEFAULT_COLUMNS: Int = 3
}
