/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Coordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** What the Astronomy screen needs from the preferences and the chosen place (T-600, T-603); bound in `:app`. */
data class AstronomySettings(
    /** Display name of the place, already localized. */
    val placeName: String,
    val place: Coordinates,
    /** Civil time zone of the place; times and the selected day are shown in it. */
    val timeZone: TimeZone,
    /** App language: dates, digits and direction. */
    val language: LanguageSpec,
    /** Calendar of dates, of the date picker and of the "Moon in Scorpio" year. */
    val calendar: CalendarArithmetic = PersianCalendarSystem,
    /** How "the Moon in Scorpio" is decided (docs/DATA_TODO.md DT-013). */
    val scorpioSystem: ZodiacSystem = ZodiacSystem.IAU_CONSTELLATION,
)

/** The current [AstronomySettings]; emits `null` while no place has been chosen. */
fun interface AstronomySettingsSource {
    fun settings(): Flow<AstronomySettings?>
}
