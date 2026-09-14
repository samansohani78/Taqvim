/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One settings item: its title, a summary of its value (and tab while searching) and a switch where it has one. */
@Composable
internal fun SettingsRowItem(
    row: SettingsRow,
    highlighted: Boolean,
    showTab: Boolean,
    onClick: (SettingsItemId) -> Unit,
) {
    val value = row.value
    val interaction =
        when (value) {
            is RowValue.Switch -> {
                Modifier.toggleable(value = value.checked, role = Role.Switch, onValueChange = { onClick(row.id) })
            }

            is RowValue.Action -> {
                Modifier.clickable(enabled = value.enabled) { onClick(row.id) }
            }

            else -> {
                Modifier.clickable { onClick(row.id) }
            }
        }
    val background = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .then(interaction)
            .semantics { selected = highlighted }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val enabled = (value as? RowValue.Action)?.enabled ?: true
            Text(
                stringResource(row.id.title),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            summary(value, showTab, row.id)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value is RowValue.Switch) Switch(checked = value.checked, onCheckedChange = null)
    }
}

@Composable
private fun summary(
    value: RowValue,
    showTab: Boolean,
    id: SettingsItemId,
): String? {
    val parts =
        buildList {
            if (showTab) add(stringResource(id.tab.title))
            if (value is RowValue.Chosen) value.options.forEach { add(optionText(it)) }
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.settings_list_separator))
}

@Composable
private fun optionText(option: SettingsOption): String = option.text ?: stringResource(option.label)

/** Chooses one option (closing on choice) or toggles several (closed with "Done"). */
@Composable
internal fun ChoiceDialogView(
    dialog: ChoiceDialog,
    actions: SettingsHomeActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDialogDismissed,
        title = { Text(stringResource(dialog.id.title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                dialog.options.forEach { option ->
                    OptionRow(option, option.key in dialog.selected, dialog.multiple) {
                        actions.onOptionClicked(option.key)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onDialogDismissed) { Text(stringResource(R.string.settings_dialog_done)) }
        },
    )
}

/** Asks before a switch with a warning turns on; "Turn on" accepts, "Cancel" or dismissing leaves it off. */
@Composable
internal fun ToggleConfirmationView(
    confirmation: ToggleConfirmation,
    actions: SettingsHomeActions,
) {
    AlertDialog(
        onDismissRequest = actions.onConfirmationDismissed,
        title = { Text(stringResource(confirmation.id.title)) },
        text = { Text(stringResource(confirmation.warning)) },
        confirmButton = {
            TextButton(onClick = actions.onConfirmationAccepted) {
                Text(stringResource(R.string.settings_dialog_turn_on))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onConfirmationDismissed) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        },
    )
}

@Composable
private fun OptionRow(
    option: SettingsOption,
    chosen: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
) {
    val interaction =
        if (multiple) {
            Modifier.toggleable(value = chosen, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
        }
    Row(
        Modifier.fillMaxWidth().then(interaction).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (multiple) {
            Checkbox(checked = chosen, onCheckedChange = null)
        } else {
            RadioButton(selected = chosen, onClick = null)
        }
        Text(optionText(option), style = MaterialTheme.typography.bodyLarge)
    }
}
