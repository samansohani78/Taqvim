/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** State of the athan settings (T-1101). */
data class AthanSettingsUiState(
    val loading: Boolean = true,
    val alerts: ImmutableList<PrayerAlertRow> = persistentListOf(),
    val sound: SoundState = SoundState(),
    val vibrate: Boolean = true,
    val bypassDndForFajr: Boolean = false,
    /** Whether the Do Not Disturb bypass can be changed: only while the Fajr athan is on. */
    val bypassAvailable: Boolean = false,
    val volumePercent: Int = 0,
    /** [volumePercent] in the app's digits. */
    val volumeText: String = "",
    val useIranTime: Boolean = false,
    /** Whether an athan is on while exact alarms are not allowed, so it may sound late. */
    val exactAlarmsBlocked: Boolean = false,
)

/** The athan of one prayer. */
data class PrayerAlertRow(
    val prayer: AthanPrayerKind,
    val enabled: Boolean,
    val gapMinutes: Int,
    /** The size of [gapMinutes] in the app's digits. */
    val gapText: String,
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean,
)

/** The athan sound. */
data class SoundState(
    /** Whether a picked sound is used instead of the default alarm sound. */
    val custom: Boolean = false,
    /** Name of the picked sound, or `null` when it has none. */
    val name: String? = null,
    val previewing: Boolean = false,
    /** Whether the last picked file could not be used. */
    val unreadable: Boolean = false,
)

/** User actions of the athan settings. */
@Immutable
data class AthanSettingsActions(
    val onAlertToggled: (AthanPrayerKind, Boolean) -> Unit = { _, _ -> },
    /** Moves the athan of a prayer by the given minutes (negative is earlier). */
    val onGapStep: (AthanPrayerKind, Int) -> Unit = { _, _ -> },
    val onPickSound: () -> Unit = {},
    val onUseDefaultSound: () -> Unit = {},
    val onPreview: () -> Unit = {},
    val onVolumeChanged: (Int) -> Unit = {},
    val onVibrateChanged: (Boolean) -> Unit = {},
    val onBypassDndChanged: (Boolean) -> Unit = {},
    val onIranTimeChanged: (Boolean) -> Unit = {},
    val onAllowExactAlarms: () -> Unit = {},
    /** Opens the system screen that grants Do Not Disturb access (Fajr bypass, T-1101). */
    val onOpenDndAccess: () -> Unit = {},
)
