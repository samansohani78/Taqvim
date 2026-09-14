/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import kotlin.math.roundToInt

/** Athan settings (T-1101), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun AthanSettingsScreen(
    state: AthanSettingsUiState,
    actions: AthanSettingsActions,
    modifier: Modifier = Modifier,
    /** Whether the app lacks Do Not Disturb access, which the Fajr bypass needs. */
    dndAccessMissing: Boolean = false,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.settings_athan_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.settings_athan_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                AthanContent(state, actions, dndAccessMissing)
            }
        }
    }
}

@Composable
private fun AthanContent(
    state: AthanSettingsUiState,
    actions: AthanSettingsActions,
    dndAccessMissing: Boolean,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.exactAlarmsBlocked) {
            WarningBanner(
                R.string.settings_athan_exact_blocked,
                R.string.settings_athan_exact_allow,
                actions.onAllowExactAlarms,
            )
        }
        SectionHeading(R.string.settings_athan_prayers)
        state.alerts.forEach { PrayerAlertItem(it, actions) }
        SectionHeading(R.string.settings_athan_sound)
        SoundCard(state.sound, actions)
        VolumeControl(state.volumePercent, state.volumeText, actions.onVolumeChanged)
        SectionHeading(R.string.settings_athan_options)
        AthanOptions(state, actions, dndAccessMissing)
    }
}

/** Vibration, the Fajr Do Not Disturb bypass (with its access warning) and Iran time. */
@Composable
private fun AthanOptions(
    state: AthanSettingsUiState,
    actions: AthanSettingsActions,
    dndAccessMissing: Boolean,
) {
    SwitchItem(stringResource(R.string.settings_athan_vibrate), null, state.vibrate, actions.onVibrateChanged)
    SwitchItem(
        title = stringResource(R.string.settings_athan_bypass_dnd),
        detail =
            stringResource(
                if (state.bypassAvailable) {
                    R.string.settings_athan_bypass_dnd_detail
                } else {
                    R.string.settings_athan_bypass_dnd_needs_fajr
                },
            ),
        checked = state.bypassDndForFajr,
        onChange = actions.onBypassDndChanged,
        enabled = state.bypassAvailable,
    )
    if (state.bypassDndForFajr && dndAccessMissing) {
        WarningBanner(
            R.string.settings_athan_dnd_access_missing,
            R.string.settings_athan_dnd_access_allow,
            actions.onOpenDndAccess,
        )
    }
    SwitchItem(
        title = stringResource(R.string.settings_athan_iran_time),
        detail = stringResource(R.string.settings_athan_iran_time_detail),
        checked = state.useIranTime,
        onChange = actions.onIranTimeChanged,
    )
}

/** A warning with a button that fixes it, announced when it appears. */
@Composable
private fun WarningBanner(
    @StringRes message: Int,
    @StringRes button: Int,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(message), color = MaterialTheme.colorScheme.onErrorContainer)
        TextButton(onClick = onClick) { Text(stringResource(button)) }
    }
}

@Composable
private fun SoundCard(
    sound: SoundState,
    actions: AthanSettingsActions,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val name =
            when {
                !sound.custom -> stringResource(R.string.settings_athan_sound_default)
                else -> sound.name ?: stringResource(R.string.settings_athan_sound_unnamed)
            }
        Text(name, style = MaterialTheme.typography.titleMedium)
        if (sound.unreadable) {
            Text(
                stringResource(R.string.settings_athan_sound_unreadable),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = actions.onPickSound) {
                Text(stringResource(R.string.settings_athan_sound_choose))
            }
            OutlinedButton(onClick = actions.onUseDefaultSound, enabled = sound.custom) {
                Text(stringResource(R.string.settings_athan_sound_use_default))
            }
            TextButton(onClick = actions.onPreview) {
                val previewLabel =
                    if (sound.previewing) R.string.settings_athan_preview_stop else R.string.settings_athan_preview_play
                Text(stringResource(previewLabel))
            }
        }
    }
}

@Composable
private fun VolumeControl(
    percent: Int,
    text: String,
    onChange: (Int) -> Unit,
) {
    val label = stringResource(R.string.settings_athan_volume)
    val value = stringResource(R.string.settings_athan_volume_value, text)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = percent.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..MAX_VOLUME,
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = label
                    stateDescription = value
                },
        )
    }
}

@Composable
private fun SectionHeading(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

private const val MAX_VOLUME = 100f
