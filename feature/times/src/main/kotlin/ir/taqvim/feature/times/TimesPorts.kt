/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** What the Times tab needs from the preferences and the chosen place (T-600, T-603); bound in `:app`. */
data class TimesSettings(
    /** Display name of the place, already localized. */
    val placeName: String,
    val place: Coordinates,
    /** Civil time zone of the place; prayer times are shown in it. */
    val timeZone: TimeZone,
    val prayer: PrayerSettings,
    /** App language: date titles, digits, durations and the report direction. */
    val language: LanguageSpec,
    /** Calendar of the day title and of the monthly report. */
    val calendar: CalendarArithmetic = PersianCalendarSystem,
)

/** The current [TimesSettings]; emits `null` while no place has been chosen. */
fun interface TimesSettingsSource {
    fun settings(): Flow<TimesSettings?>
}
