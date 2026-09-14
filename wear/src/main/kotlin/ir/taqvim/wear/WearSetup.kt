/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.UserPreferences
import java.util.Locale
import kotlinx.datetime.TimeZone

/** A place on the watch: its display [name], position and IANA time zone. */
data class WearPlace(
    val name: String,
    val coordinates: Coordinates,
    val timeZone: TimeZone,
)

/** What every watch surface needs from the preferences stored on the watch (standalone watch, ADR-0019). */
data class WearSetup(
    val language: LanguageSpec,
    val numerals: NumeralSystem,
    /** The user's calendars that can be computed, primary first; never empty. */
    val calendars: List<CalendarArithmetic>,
    val islamicVariant: IslamicVariant,
    val weekStart: Weekday,
    val weekend: Set<Weekday>,
    val enabledSources: Set<EventSource>,
    val prayer: PrayerSettings,
    val place: WearPlace?,
    val deviceZone: TimeZone,
) {
    /** The primary calendar. */
    val primary: CalendarArithmetic
        get() = calendars.first()

    /** The time zone of the chosen place, or of the watch when no place is chosen. */
    val zone: TimeZone
        get() = place?.timeZone ?: deviceZone
}

/** Calendar arithmetic for the watch, with the user's Islamic variant; Nepali waits for T-105. */
object WearCalendars {
    fun arithmeticFor(
        system: CalendarSystem,
        variant: IslamicVariant,
    ): CalendarArithmetic? =
        when (system) {
            CalendarSystem.PERSIAN -> PersianCalendarSystem
            CalendarSystem.ISLAMIC -> IslamicCalendarSelection.calendarFor(variant)
            CalendarSystem.GREGORIAN -> GregorianCalendarSystem
            CalendarSystem.NEPALI -> null
        }
}

/**
 * The watch setup of these preferences. [cityName] names a catalog city in a language (`null` when unknown); places
 * whose stored zone this device does not know are ignored.
 */
fun UserPreferences.toWearSetup(
    deviceZone: TimeZone,
    cityName: (cityId: Long, languageCode: String) -> String?,
): WearSetup {
    val language =
        LanguageTable.forCode(languageCode)
            ?: requireNotNull(LanguageTable.forCode(UserPreferences.FALLBACK_LANGUAGE)) { "no fallback language" }
    val available = calendars.distinct().mapNotNull { WearCalendars.arithmeticFor(it, islamicVariant) }
    return WearSetup(
        language = language,
        numerals = numerals,
        calendars = available.ifEmpty { listOf(PersianCalendarSystem) },
        islamicVariant = islamicVariant,
        weekStart = weekStart,
        weekend = weekend,
        enabledSources = app.enabledEventSources,
        prayer = PrayerSettings(method = prayerMethod, asr = asrJuristic, highLatitude = app.highLatitudeRule),
        place = place?.toWearPlace(language.code, cityName),
        deviceZone = deviceZone,
    )
}

private fun ChosenPlace.toWearPlace(
    languageCode: String,
    cityName: (Long, String) -> String?,
): WearPlace? {
    val zone = runCatching { TimeZone.of(zoneId) }.getOrNull() ?: return null
    val label = cityId?.let { cityName(it, languageCode) } ?: name ?: coordinatesLabel(coordinates)
    return WearPlace(label, coordinates, zone)
}

/** A label for a place without a name: its coordinates to two decimals. */
internal fun coordinatesLabel(coordinates: Coordinates): String =
    String.format(Locale.ROOT, "%.2f, %.2f", coordinates.latitude, coordinates.longitude)
