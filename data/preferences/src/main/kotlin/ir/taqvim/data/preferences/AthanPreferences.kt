/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.data.preferences.proto.AthanPrayerAlertProto
import ir.taqvim.data.preferences.proto.AthanPrayerProto
import ir.taqvim.data.preferences.proto.AthanSettingsProto

/** The prayers that can have an athan. */
enum class AthanPrayer {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

/** The athan of one prayer: whether it sounds, and [gapMinutes] from the prayer time (negative is before). */
data class AthanAlert(
    val enabled: Boolean,
    val gapMinutes: Int,
) {
    init {
        require(gapMinutes in GAP_RANGE) { "gap must be within $GAP_RANGE minutes" }
    }

    companion object {
        /** Allowed gap between the prayer time and the athan, in minutes. */
        val GAP_RANGE: IntRange = -60..60

        /** No athan, at the prayer time. */
        val OFF: AthanAlert = AthanAlert(enabled = false, gapMinutes = 0)
    }
}

/** A sound picked by the user through the system file picker. */
data class AthanSound(
    /** Content URI of the sound. */
    val uri: String,
    /** Display name, or `null` when unknown. */
    val name: String?,
) {
    init {
        require(uri.isNotBlank()) { "sound uri must not be blank" }
    }
}

/** Athan settings (T-1101). */
data class AthanPreferences(
    /** One alert per [AthanPrayer]. */
    val alerts: Map<AthanPrayer, AthanAlert>,
    /** The picked sound, or `null` for the default alarm sound. */
    val sound: AthanSound?,
    val vibrate: Boolean,
    /** Whether the Fajr athan may sound while Do Not Disturb is on. */
    val bypassDndForFajr: Boolean,
    val volumePercent: Int,
    /** Whether athan times use Iran Standard Time (UTC+03:30) instead of the time zone rules of the place. */
    val useIranTime: Boolean,
) {
    init {
        require(alerts.keys == AthanPrayer.entries.toSet()) { "an alert is required for every prayer" }
        require(volumePercent in VOLUME_RANGE) { "volume must be within $VOLUME_RANGE" }
    }

    companion object {
        /** Allowed volume, in percent of the alarm stream. */
        val VOLUME_RANGE: IntRange = 0..100

        private const val DEFAULT_VOLUME = 80

        /**
         * Product defaults, the same for every language (ADR-0007 has no athan convention): no athan until the user
         * turns one on, the default sound at 80 %, vibration on, no Do Not Disturb bypass and the place's time zone.
         */
        val DEFAULT: AthanPreferences =
            AthanPreferences(
                alerts = AthanPrayer.entries.associateWith { AthanAlert.OFF },
                sound = null,
                vibrate = true,
                bypassDndForFajr = false,
                volumePercent = DEFAULT_VOLUME,
                useIranTime = false,
            )
    }
}

private const val ATHAN_PRAYER = "ATHAN_PRAYER_"

/**
 * The stored athan settings. Prayers without a stored alert are off; unknown prayers and later duplicates are ignored;
 * out-of-range gaps and volumes are clamped, and a blank sound reads as the default sound.
 */
internal fun AthanSettingsProto.toDomain(): AthanPreferences {
    val stored =
        alertsList
            .mapNotNull { alert ->
                AthanPrayer.entries.firstOrNull { ATHAN_PRAYER + it.name == alert.prayer.name }?.let { it to alert }
            }.distinctBy { it.first }
            .toMap()
    return AthanPreferences(
        alerts =
            AthanPrayer.entries.associateWith { prayer ->
                stored[prayer]?.let { AthanAlert(it.enabled, it.gapMinutes.coerceIn(AthanAlert.GAP_RANGE)) }
                    ?: AthanAlert.OFF
            },
        sound = soundUri.takeIf { it.isNotBlank() }?.let { AthanSound(it, soundName.ifBlank { null }) },
        vibrate = vibrate,
        bypassDndForFajr = bypassDndForFajr,
        volumePercent = volumePercent.coerceIn(AthanPreferences.VOLUME_RANGE),
        useIranTime = useIranTime,
    )
}

internal fun AthanPreferences.toProto(): AthanSettingsProto =
    AthanSettingsProto
        .newBuilder()
        .addAllAlerts(
            AthanPrayer.entries.map { prayer ->
                val alert = alerts.getValue(prayer)
                AthanPrayerAlertProto
                    .newBuilder()
                    .setPrayer(AthanPrayerProto.valueOf(ATHAN_PRAYER + prayer.name))
                    .setEnabled(alert.enabled)
                    .setGapMinutes(alert.gapMinutes)
                    .build()
            },
        ).setSoundUri(sound?.uri.orEmpty())
        .setSoundName(sound?.name.orEmpty())
        .setVibrate(vibrate)
        .setBypassDndForFajr(bypassDndForFajr)
        .setVolumePercent(volumePercent)
        .setUseIranTime(useIranTime)
        .build()
