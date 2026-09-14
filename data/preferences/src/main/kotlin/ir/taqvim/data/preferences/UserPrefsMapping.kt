/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.preferences.proto.AsrJuristicProto
import ir.taqvim.data.preferences.proto.CalendarSystemProto
import ir.taqvim.data.preferences.proto.IslamicVariantProto
import ir.taqvim.data.preferences.proto.NumeralSystemProto
import ir.taqvim.data.preferences.proto.PrayerMethodProto
import ir.taqvim.data.preferences.proto.ThemeModeProto
import ir.taqvim.data.preferences.proto.UserPrefs
import ir.taqvim.data.preferences.proto.WeekdayProto

private const val CALENDAR = "CALENDAR_SYSTEM_"
private const val NUMERAL = "NUMERAL_SYSTEM_"
private const val WEEKDAY = "WEEKDAY_"
private const val PRAYER = "PRAYER_METHOD_"
private const val ASR = "ASR_JURISTIC_"
private const val VARIANT = "ISLAMIC_VARIANT_"
private const val THEME = "THEME_MODE_"

/** The domain value whose name equals [protoName] without [prefix]; `null` for UNSPECIFIED or unknown values. */
private inline fun <reified T : Enum<T>> domain(
    protoName: String,
    prefix: String,
): T? = enumValues<T>().firstOrNull { prefix + it.name == protoName }

/**
 * Typed view of the stored [UserPrefs]. Unspecified or unknown fields take the defaults of the stored language, so a
 * partially written or older message still yields complete preferences.
 */
fun UserPrefs.toDomain(): UserPreferences {
    val defaults = UserPreferences.defaultsFor(languageCode.ifBlank { UserPreferences.FALLBACK_LANGUAGE })
    val calendarsList = calendarsList.mapNotNull { domain<CalendarSystem>(it.name, CALENDAR) }.distinct()
    val weekendSet = weekendList.mapNotNull { domain<Weekday>(it.name, WEEKDAY) }.toSet()
    return UserPreferences(
        languageCode = defaults.languageCode,
        calendars = calendarsList.ifEmpty { defaults.calendars },
        numerals = domain<NumeralSystem>(numerals.name, NUMERAL) ?: defaults.numerals,
        weekStart = domain<Weekday>(weekStart.name, WEEKDAY) ?: defaults.weekStart,
        weekend = weekendSet.ifEmpty { defaults.weekend },
        prayerMethod = domain<PrayerMethod>(prayerMethod.name, PRAYER) ?: defaults.prayerMethod,
        asrJuristic = domain<AsrJuristic>(asrJuristic.name, ASR) ?: defaults.asrJuristic,
        islamicVariant = domain<IslamicVariant>(islamicVariant.name, VARIANT) ?: defaults.islamicVariant,
        themeMode = domain<ThemeMode>(themeMode.name, THEME) ?: defaults.themeMode,
        hijriOffsetDays = hijriOffsetDays,
        hijriOffsetSetAtEpochMillis = hijriOffsetSetAtEpochMillis.takeIf { it != 0L },
        place = if (hasPlace()) place.toDomainOrNull() else null,
        athan = if (hasAthan()) athan.toDomain() else AthanPreferences.DEFAULT,
        app = if (hasAppSettings()) appSettings.toDomain() else AppSettings.DEFAULT,
    )
}

/** The proto for these preferences at [schemaVersion]. */
fun UserPreferences.toProto(schemaVersion: Int = UserPrefsMigration.CURRENT_SCHEMA_VERSION): UserPrefs =
    UserPrefs
        .newBuilder()
        .setSchemaVersion(schemaVersion)
        .setLanguageCode(languageCode)
        .addAllCalendars(calendars.map { CalendarSystemProto.valueOf(CALENDAR + it.name) })
        .setNumerals(NumeralSystemProto.valueOf(NUMERAL + numerals.name))
        .setWeekStart(WeekdayProto.valueOf(WEEKDAY + weekStart.name))
        .addAllWeekend(weekend.sortedBy { it.ordinal }.map { WeekdayProto.valueOf(WEEKDAY + it.name) })
        .setPrayerMethod(PrayerMethodProto.valueOf(PRAYER + prayerMethod.name))
        .setAsrJuristic(AsrJuristicProto.valueOf(ASR + asrJuristic.name))
        .setIslamicVariant(IslamicVariantProto.valueOf(VARIANT + islamicVariant.name))
        .setThemeMode(ThemeModeProto.valueOf(THEME + themeMode.name))
        .setHijriOffsetDays(hijriOffsetDays)
        .setHijriOffsetSetAtEpochMillis(hijriOffsetSetAtEpochMillis ?: 0L)
        .apply { this@toProto.place?.let { setPlace(it.toProto()) } }
        .setAthan(athan.toProto())
        .setAppSettings(app.toProto())
        .build()
