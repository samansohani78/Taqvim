/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The athan switch of one prayer, with its gap controls while it is on. */
@Composable
internal fun PrayerAlertItem(
    row: PrayerAlertRow,
    actions: AthanSettingsActions,
) {
    val prayer = stringResource(row.prayer.label)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SwitchItem(
            title = prayer,
            detail = if (row.enabled) gapText(row) else null,
            checked = row.enabled,
            onChange = { actions.onAlertToggled(row.prayer, it) },
        )
        if (row.enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    symbol = R.string.settings_athan_gap_minus,
                    description = stringResource(R.string.settings_athan_gap_earlier, prayer),
                    enabled = row.canMoveEarlier,
                    onClick = { actions.onGapStep(row.prayer, -GAP_STEP_MINUTES) },
                )
                StepButton(
                    symbol = R.string.settings_athan_gap_plus,
                    description = stringResource(R.string.settings_athan_gap_later, prayer),
                    enabled = row.canMoveLater,
                    onClick = { actions.onGapStep(row.prayer, GAP_STEP_MINUTES) },
                )
            }
        }
    }
}

/** A row with a title, optional detail and a switch; the whole row toggles. */
@Composable
internal fun SwitchItem(
    title: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun StepButton(
    @StringRes symbol: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(stringResource(symbol), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun gapText(row: PrayerAlertRow): String =
    when {
        row.gapMinutes < 0 -> stringResource(R.string.settings_athan_gap_before, row.gapText)
        row.gapMinutes > 0 -> stringResource(R.string.settings_athan_gap_after, row.gapText)
        else -> stringResource(R.string.settings_athan_gap_on_time)
    }

@get:StringRes
internal val AthanPrayerKind.label: Int
    get() =
        when (this) {
            AthanPrayerKind.FAJR -> R.string.settings_athan_prayer_fajr
            AthanPrayerKind.DHUHR -> R.string.settings_athan_prayer_dhuhr
            AthanPrayerKind.ASR -> R.string.settings_athan_prayer_asr
            AthanPrayerKind.MAGHRIB -> R.string.settings_athan_prayer_maghrib
            AthanPrayerKind.ISHA -> R.string.settings_athan_prayer_isha
        }

private const val GAP_STEP_MINUTES = 1
