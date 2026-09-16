/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/** The event editor (T-1000), stateless apart from its confirmation dialogs and open picker. */
@Composable
fun EventEditorScreen(
    state: EventEditorUiState,
    actions: EventEditorActions,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    val editing = content as? EditorContent.Editing
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val title = if (editing?.isNew == false) R.string.events_editor_edit_title else R.string.events_editor_new_title
    val requestClose = { decision: CloseDecision ->
        when (decision) {
            CloseDecision.CLOSE -> actions.onDiscard()
            CloseDecision.CONFIRM -> confirmDiscard = true
            CloseDecision.WAIT -> Unit
        }
    }
    // B10: system Back (predictive or not) follows the same rule as Cancel; with nothing to lose it is not
    // intercepted, so the navigation's own back and its predictive animation apply.
    val backDecision = editing?.closeDecision ?: CloseDecision.CLOSE
    BackHandler(enabled = backDecision != CloseDecision.CLOSE) { requestClose(backDecision) }
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(title)) },
        bottomBar = {
            editing?.let {
                EditorBottomBar(
                    editing = it,
                    onSave = actions.onSave,
                    onDiscard = { requestClose(it.closeDecision) },
                    onDelete = { confirmDelete = true },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            EditorBody(content, actions)
        }
    }
    if (confirmDiscard) {
        ConfirmDialog(DialogTexts.DISCARD, onConfirm = actions.onDiscard, onClose = { confirmDiscard = false })
    }
    if (confirmDelete) {
        val texts = if (editing?.occurrenceOnly == true) DialogTexts.CANCEL_OCCURRENCE else DialogTexts.DELETE
        ConfirmDialog(texts, onConfirm = actions.onDelete, onClose = { confirmDelete = false })
    }
}

@Composable
private fun EditorBody(
    content: EditorContent,
    actions: EventEditorActions,
) {
    when (content) {
        EditorContent.Loading -> {
            val description = stringResource(R.string.events_loading)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = description })
            }
        }

        EditorContent.NotFound -> {
            EmptyState(
                title = stringResource(R.string.events_not_found_title),
                message = stringResource(R.string.events_not_found_message),
            )
        }

        EditorContent.ChoosingScope -> {
            ScopeDialog(actions)
        }

        is EditorContent.Editing -> {
            EditorFields(content, actions)
        }

        is EditorContent.Finished -> {
            // The route closes the editor; nothing is shown meanwhile.
            Spacer(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun EditorFields(
    editing: EditorContent.Editing,
    actions: EventEditorActions,
) {
    var picker by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (editing.occurrenceOnly) Text(stringResource(R.string.events_occurrence_note))
        TitleField(editing, actions.onIntent)
        DatePhraseField(editing, actions)
        ScheduleSection(editing, actions.onIntent, onPick = { picker = it })
        // One occurrence keeps the series' repetition and reminders (ADR-0034).
        if (!editing.occurrenceOnly) {
            RepeatSection(editing, actions.onIntent, onPick = { picker = it })
            ReminderSection(editing, actions.onIntent)
        }
        ColorSection(editing, actions.onIntent)
        NotesAndLinkFields(editing, actions.onIntent)
    }
    picker?.let { target -> EditorPicker(target, editing, actions.onIntent, onClose = { picker = null }) }
}

@Composable
private fun EditorBottomBar(
    editing: EditorContent.Editing,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val problem =
            when {
                editing.storeFailed -> R.string.events_store_failed
                editing.errors.isNotEmpty() -> R.string.events_errors_summary
                else -> null
            }
        problem?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDiscard, enabled = !editing.busy) { Text(stringResource(R.string.events_cancel)) }
            if (!editing.isNew) {
                val label = if (editing.occurrenceOnly) R.string.events_cancel_occurrence else R.string.events_delete
                TextButton(onClick = onDelete, enabled = !editing.busy) { Text(stringResource(label)) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSave, enabled = !editing.busy) { Text(stringResource(R.string.events_save)) }
        }
    }
}

/** Texts of a confirmation dialog. */
private enum class DialogTexts(
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
    @param:StringRes val confirm: Int,
    @param:StringRes val dismiss: Int,
) {
    DISCARD(
        R.string.events_discard_title,
        R.string.events_discard_message,
        R.string.events_discard_confirm,
        R.string.events_keep_editing,
    ),
    DELETE(
        R.string.events_delete_title,
        R.string.events_delete_message,
        R.string.events_delete_confirm,
        R.string.events_cancel,
    ),
    CANCEL_OCCURRENCE(
        R.string.events_cancel_occurrence_title,
        R.string.events_cancel_occurrence_message,
        R.string.events_cancel_occurrence_confirm,
        R.string.events_keep_editing,
    ),
}

/** Asks whether to change one occurrence or the whole series; dismissing it closes the editor (ADR-0034). */
@Composable
private fun ScopeDialog(actions: EventEditorActions) {
    AlertDialog(
        onDismissRequest = actions.onDiscard,
        title = { Text(stringResource(R.string.events_scope_title)) },
        text = { Text(stringResource(R.string.events_scope_message)) },
        confirmButton = {
            TextButton(onClick = { actions.onChooseScope(true) }) { Text(stringResource(R.string.events_scope_this)) }
        },
        dismissButton = {
            TextButton(onClick = { actions.onChooseScope(false) }) { Text(stringResource(R.string.events_scope_all)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    texts: DialogTexts,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(texts.title)) },
        text = { Text(stringResource(texts.message)) },
        confirmButton = {
            TextButton(onClick = {
                onClose()
                onConfirm()
            }) { Text(stringResource(texts.confirm)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(texts.dismiss)) } },
    )
}
