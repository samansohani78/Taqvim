/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.ThemeMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The JSON document of a backup (format version [FORMAT_VERSION]); field names are part of the file format. */
@Serializable
internal data class BackupDocument(
    val format: String = FORMAT_NAME,
    val formatVersion: Int = FORMAT_VERSION,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val preferences: PreferencesRecord,
    val data: DataRecord = DataRecord(),
) {
    companion object {
        const val FORMAT_NAME: String = "taqvim-backup"
        const val FORMAT_VERSION: Int = 1

        /** Unknown keys from newer versions are ignored; defaults are written so every field is explicit. */
        val json: Json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}

@Serializable
internal data class PreferencesRecord(
    val languageCode: String,
    val calendars: List<CalendarSystem>,
    val numerals: NumeralSystem,
    val weekStart: Weekday,
    val weekend: Set<Weekday>,
    val prayerMethod: PrayerMethod,
    val asrJuristic: AsrJuristic,
    val islamicVariant: IslamicVariant,
    val themeMode: ThemeMode,
    val hijriOffsetDays: Int = 0,
    val hijriOffsetSetAtEpochMillis: Long? = null,
    /** The chosen place (T-1502); absent in backups made before it existed. */
    val place: PlaceRecord? = null,
    /** Settings of the settings screens (T-1500); absent in backups made before them. */
    val app: AppSettingsRecord? = null,
    /** Athan settings (T-1101); absent in backups made before them. */
    val athan: AthanRecord? = null,
)

/**
 * The backed-up [ir.taqvim.data.preferences.AthanPreferences]. The picked sound stays on its device: read access to a
 * picked document is granted to that device only.
 */
@Serializable
internal data class AthanRecord(
    val alerts: List<AthanAlertRecord>,
    val vibrate: Boolean,
    val bypassDndForFajr: Boolean,
    val volumePercent: Int,
    val useIranTime: Boolean,
)

@Serializable
internal data class AthanAlertRecord(
    val prayer: AthanPrayer,
    val enabled: Boolean,
    val gapMinutes: Int,
)

/**
 * The backed-up [ir.taqvim.data.preferences.AppSettings]. Recent searches and level calibration stay on the device
 * they belong to and are not written.
 */
@Serializable
internal data class AppSettingsRecord(
    val dynamicColor: Boolean,
    val highContrast: Boolean,
    val boldText: Boolean,
    val gradient: Boolean,
    val showWeekNumbers: Boolean,
    val enabledEventSources: Set<EventSource>,
    val highLatitudeRule: HighLatitudeRule,
    val subscriptionsNetworkAllowed: Boolean,
    val persistentNotification: Boolean,
    val rememberRecentSearches: Boolean,
    val timeZoneBoard: List<String> = emptyList(),
    /** All-day reminder time (T-1001); absent in backups made before it existed. */
    val allDayReminderMinute: Int = AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE,
)

@Serializable
internal data class PlaceRecord(
    val source: PlaceSource,
    val cityId: Long? = null,
    val name: String? = null,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
)

@Serializable
internal data class DataRecord(
    val personalEvents: List<EventRecord> = emptyList(),
    val eventRecurrences: List<RecurrenceRecord> = emptyList(),
    val reminders: List<ReminderRecord> = emptyList(),
    val shiftRotations: List<ShiftRotationRecord> = emptyList(),
    val shiftRotationRecords: List<ShiftDayRecord> = emptyList(),
    val icsSubscriptions: List<IcsSubscriptionRecord> = emptyList(),
    val workdayProfiles: List<WorkdayProfileRecord> = emptyList(),
    /** Reminders before official events (T-1002); absent in backups made before them. */
    val officialReminders: List<OfficialReminderRecord> = emptyList(),
)

@Serializable
internal data class OfficialReminderRecord(
    val id: Long,
    val eventId: String,
    val daysBefore: Int,
    val enabled: Boolean = true,
)

@Serializable
internal data class EventRecord(
    val id: Long,
    val title: String,
    val notes: String = "",
    val calendarSystem: CalendarSystem,
    val startJdn: Long,
    val startMinute: Int? = null,
    val endJdn: Long,
    val endMinute: Int? = null,
    val timeZoneId: String,
    val colorArgb: Int? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val icsUid: String? = null,
    val sourceLink: String? = null,
)

@Serializable
internal data class WeekdayNumRecord(
    val weekday: Weekday,
    val ordinal: Int? = null,
)

@Serializable
internal data class RecurrenceRecord(
    val eventId: Long,
    val frequency: Frequency,
    val interval: Int,
    val count: Int? = null,
    val untilJdn: Long? = null,
    val byDay: List<WeekdayNumRecord> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val calendarSystem: CalendarSystem,
    val invalidDates: InvalidDatePolicy,
    val weekStart: Weekday,
)

@Serializable
internal data class ReminderRecord(
    val id: Long,
    val eventId: Long,
    val minutesBefore: Int,
    val enabled: Boolean = true,
)

@Serializable
internal data class ShiftRotationRecord(
    val id: Long,
    val name: String,
    val anchorJdn: Long,
    val pattern: List<String>,
    val isActive: Boolean = true,
)

@Serializable
internal data class ShiftDayRecord(
    val rotationId: Long,
    val jdn: Long,
    val shift: String,
    val note: String = "",
)

/** Fetch state (`lastFetched`, `etag`) is not backed up: restored subscriptions are fetched again. */
@Serializable
internal data class IcsSubscriptionRecord(
    val id: Long,
    val url: String,
    val displayName: String,
    val colorArgb: Int? = null,
    val enabled: Boolean = true,
    val refreshIntervalMinutes: Int,
)

@Serializable
internal data class LeaveRangeRecord(
    val firstJdn: Long,
    val lastJdn: Long,
)

@Serializable
internal data class WorkdayProfileRecord(
    val id: Long,
    val name: String,
    val weekend: Set<Weekday>,
    val holidaySources: Set<EventSource>,
    val halfDays: HalfDayPolicy,
    val personalLeave: List<LeaveRangeRecord> = emptyList(),
    val isDefault: Boolean = false,
)
