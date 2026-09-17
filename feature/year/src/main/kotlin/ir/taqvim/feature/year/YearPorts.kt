/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import kotlinx.coroutines.flow.Flow

/** The preferences the year view reacts to (T-805). */
data class YearSettings(
    /** The user's calendars in order; the first available one is shown first. */
    val calendars: List<CalendarSystem>,
    val weekStart: Weekday,
    val islamicVariant: IslamicVariant,
    /** App language code (e.g. `fa`): month names, digits and spoken month summaries. */
    val languageCode: String,
    /** Optional official Iranian month starts in use (ADR-0037); `null` when every Islamic date is computed. */
    val islamicOverrides: IslamicMonthTable? = null,
)

/** Whether the civil day [jdn] is an official holiday or a weekend day. */
data class YearDay(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
)

/** The user's calendar preferences; re-emits on every change. Implemented in `:app` over the data layer. */
fun interface YearSettingsSource {
    fun settings(): Flow<YearSettings>
}

/** Holiday and weekend flags of a range of days in day order; re-emits when they change. Implemented in `:app`. */
fun interface YearDaysSource {
    fun days(range: JdnRange): Flow<List<YearDay>>
}

/** The current civil day; emits again when the day changes. Implemented in `:app`. */
fun interface YearTodaySource {
    fun today(): Flow<Jdn>
}
