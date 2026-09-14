/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.content.Context
import androidx.annotation.StringRes
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod

@StringRes
internal fun prayerLabel(prayer: WearPrayer): Int =
    when (prayer) {
        WearPrayer.FAJR -> R.string.wear_prayer_fajr
        WearPrayer.DHUHR -> R.string.wear_prayer_dhuhr
        WearPrayer.ASR -> R.string.wear_prayer_asr
        WearPrayer.MAGHRIB -> R.string.wear_prayer_maghrib
        WearPrayer.ISHA -> R.string.wear_prayer_isha
    }

internal fun Context.prayerName(prayer: WearPrayer): String = getString(prayerLabel(prayer))

@StringRes
internal fun calendarLabel(system: CalendarSystem): Int =
    when (system) {
        CalendarSystem.PERSIAN -> R.string.wear_calendar_persian
        CalendarSystem.ISLAMIC -> R.string.wear_calendar_islamic
        CalendarSystem.GREGORIAN -> R.string.wear_calendar_gregorian
        CalendarSystem.NEPALI -> R.string.wear_calendar_nepali
    }

@StringRes
internal fun methodLabel(method: PrayerMethod): Int =
    when (method) {
        PrayerMethod.MWL -> R.string.wear_method_mwl
        PrayerMethod.ISNA -> R.string.wear_method_isna
        PrayerMethod.EGYPT -> R.string.wear_method_egypt
        PrayerMethod.MAKKAH -> R.string.wear_method_makkah
        PrayerMethod.KARACHI -> R.string.wear_method_karachi
        PrayerMethod.TEHRAN -> R.string.wear_method_tehran
        PrayerMethod.JAFARI -> R.string.wear_method_jafari
        PrayerMethod.SINGAPORE -> R.string.wear_method_singapore
        PrayerMethod.FRANCE -> R.string.wear_method_france
        PrayerMethod.RUSSIA -> R.string.wear_method_russia
    }

@StringRes
internal fun fieldLabel(field: ConverterField): Int =
    when (field) {
        ConverterField.YEAR -> R.string.wear_converter_year
        ConverterField.MONTH -> R.string.wear_converter_month
        ConverterField.DAY -> R.string.wear_converter_day
    }

/** "today", "tomorrow" or "in n days" for an occasion. */
internal fun Context.daysAway(event: NextEvent): String =
    when (event.daysAway) {
        0 -> getString(R.string.wear_event_today)
        1 -> getString(R.string.wear_event_tomorrow)
        else -> resources.getQuantityString(R.plurals.wear_event_in_days, event.daysAway, event.daysAway)
    }
