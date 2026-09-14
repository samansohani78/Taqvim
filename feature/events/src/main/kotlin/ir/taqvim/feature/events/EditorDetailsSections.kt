/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.Numerals

@Composable
internal fun TitleField(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
) {
    val error = firstError(editing.errors, EditorError.TITLE_REQUIRED, EditorError.TITLE_TOO_LONG)
    OutlinedTextField(
        value = editing.form.title,
        onValueChange = { onIntent(DetailsIntent.Title(it)) },
        label = { Text(stringResource(R.string.events_title_label)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A date typed in words (F-03), applied to the start (and end of a range). */
@Composable
internal fun DatePhraseField(
    editing: EditorContent.Editing,
    actions: EventEditorActions,
) {
    val error = stringResource(R.string.events_date_phrase_unrecognized).takeIf { editing.dateTextUnrecognized }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = editing.dateText,
            onValueChange = actions.onDateTextChange,
            label = { Text(stringResource(R.string.events_date_phrase_label)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onApplyDateText() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = actions.onApplyDateText, enabled = editing.dateText.isNotBlank()) {
            Text(stringResource(R.string.events_date_phrase_apply))
        }
    }
}

@Composable
internal fun ReminderSection(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
) {
    val display = editing.display
    var choosing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(R.string.events_reminders_label)
        display.reminders.forEach { option ->
            val text = reminderText(option)
            val removeDescription = stringResource(R.string.events_reminder_remove, text)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text, Modifier.weight(1f))
                TextButton(
                    onClick = { onIntent(ReminderIntent.Remove(option.minutes)) },
                    modifier = Modifier.semantics { contentDescription = removeDescription },
                ) { Text(stringResource(R.string.events_reminder_remove_short)) }
            }
        }
        val canAdd = display.reminderChoices.isNotEmpty() && display.reminders.size < EventValidator.MAX_REMINDERS
        Box {
            TextButton(onClick = { choosing = true }, enabled = canAdd) {
                Text(stringResource(R.string.events_reminder_add))
            }
            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                display.reminderChoices.forEach { option ->
                    DropdownMenuItem(text = { Text(reminderText(option)) }, onClick = {
                        choosing = false
                        onIntent(ReminderIntent.Add(option.minutes))
                    })
                }
            }
        }
        firstError(editing.errors, EditorError.TOO_MANY_REMINDERS, EditorError.REMINDER_INVALID)?.let { ErrorLine(it) }
    }
}

@Composable
internal fun ColorSection(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
) {
    val selected = editing.form.colorArgb
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.events_color_label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().selectableGroup(),
        ) {
            ColorSwatch(null, selected == null, stringResource(R.string.events_color_none)) {
                onIntent(DetailsIntent.Color(null))
            }
            editing.display.colors.forEachIndexed { index, argb ->
                val number = Numerals.format(index + 1L, editing.display.picker.numerals)
                ColorSwatch(argb, selected == argb, stringResource(R.string.events_color_option, number)) {
                    onIntent(DetailsIntent.Color(argb))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    argb: Int?,
    isSelected: Boolean,
    description: String,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ring = if (isSelected) BorderStroke(3.dp, colors.onSurface) else BorderStroke(1.dp, colors.outline)
    Box(
        Modifier
            .size(48.dp)
            .border(ring, CircleShape)
            .background(argb?.let { Color(it) } ?: colors.surface, CircleShape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
            .semantics { contentDescription = description },
    )
}

@Composable
internal fun NotesAndLinkFields(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
) {
    val form = editing.form
    val notesError = firstError(editing.errors, EditorError.NOTES_TOO_LONG)
    OutlinedTextField(
        value = form.notes,
        onValueChange = { onIntent(DetailsIntent.Notes(it)) },
        label = { Text(stringResource(R.string.events_notes_label)) },
        isError = notesError != null,
        supportingText = notesError?.let { { Text(it) } },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    val linkError = firstError(editing.errors, EditorError.LINK_INVALID)
    OutlinedTextField(
        value = form.sourceLink,
        onValueChange = { onIntent(DetailsIntent.Link(it)) },
        label = { Text(stringResource(R.string.events_link_label)) },
        isError = linkError != null,
        supportingText = linkError?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
}
