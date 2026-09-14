/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.toImmutableList

/** Everything the athan settings screen is built from. */
internal data class AthanScreenInputs(
    val data: AthanSettingsData,
    val exactAlarmsAllowed: Boolean,
    val previewing: Boolean,
    val soundUnreadable: Boolean,
    /** Volume being dragged and not stored yet, or `null`. */
    val volumeDraft: Int?,
)

/** Turns stored athan settings into [AthanSettingsUiState]. */
internal object AthanStateMapper {
    fun state(inputs: AthanScreenInputs): AthanSettingsUiState {
        val settings = inputs.data.settings
        val numerals = inputs.data.language.numerals
        val volume = inputs.volumeDraft ?: settings.volumePercent
        val fajr = alertOf(settings, AthanPrayerKind.FAJR)
        return AthanSettingsUiState(
            loading = false,
            alerts = AthanPrayerKind.entries.map { row(it, alertOf(settings, it), numerals) }.toImmutableList(),
            sound =
                SoundState(
                    custom = settings.sound != null,
                    name = settings.sound?.name,
                    previewing = inputs.previewing,
                    unreadable = inputs.soundUnreadable,
                ),
            vibrate = settings.vibrate,
            bypassDndForFajr = settings.bypassDndForFajr,
            bypassAvailable = fajr.enabled,
            volumePercent = volume,
            volumeText = Numerals.localizeDigits(volume.toString(), numerals),
            useIranTime = settings.useIranTime,
            exactAlarmsBlocked = !inputs.exactAlarmsAllowed && settings.alerts.values.any { it.enabled },
        )
    }

    fun alertOf(
        settings: AthanSettings,
        prayer: AthanPrayerKind,
    ): PrayerAlert = settings.alerts[prayer] ?: PrayerAlert(enabled = false, gapMinutes = 0)

    private fun row(
        prayer: AthanPrayerKind,
        alert: PrayerAlert,
        numerals: NumeralSystem,
    ) = PrayerAlertRow(
        prayer = prayer,
        enabled = alert.enabled,
        gapMinutes = alert.gapMinutes,
        gapText = Numerals.localizeDigits(alert.gapMinutes.absoluteValue.toString(), numerals),
        canMoveEarlier = alert.gapMinutes > PrayerAlert.GAP_RANGE.first,
        canMoveLater = alert.gapMinutes < PrayerAlert.GAP_RANGE.last,
    )
}
