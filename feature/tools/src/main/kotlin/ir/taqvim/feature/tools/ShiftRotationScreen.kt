/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.R as SharedR
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerSheet
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import ir.taqvim.core.workdays.ShiftRotation

private val GAP = 12.dp
private val PADDING = 16.dp
private val SWATCH = 20.dp

/**
 * Shift work (F-08), stateless: the rotations the user keeps and the editor of one.
 *
 * The pattern is built by tapping the shift types in order rather than typed, because "Day, Day, Night, Night, Off,
 * Off" is a list of choices and parsing it from free text would only add ways to get it wrong.
 */
@Composable
fun ShiftRotationScreen(
    state: ShiftRotationUiState,
    actions: ShiftRotationActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.tools_shifts_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> ShiftLoading()
                state.editing != null -> RotationEditor(state.editing, actions)
                else -> RotationList(state, actions)
            }
            state.picker?.let { picker ->
                DatePickerSheet(
                    model = picker.model(anchorLabels()),
                    onConfirm = actions.onAnchorPicked,
                    onDismiss = actions.onDismissPicker,
                )
            }
        }
    }
}

@Composable
private fun ShiftLoading() {
    val description = stringResource(R.string.tools_shifts_loading)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun anchorLabels(): DatePickerLabels =
    DatePickerLabels(
        title = stringResource(R.string.tools_shifts_anchor),
        year = stringResource(SharedR.string.shared_picker_year),
        month = stringResource(SharedR.string.shared_picker_month),
        day = stringResource(SharedR.string.shared_picker_day),
        confirm = stringResource(SharedR.string.shared_picker_confirm),
        cancel = stringResource(SharedR.string.shared_picker_cancel),
    )

@Composable
private fun RotationList(
    state: ShiftRotationUiState,
    actions: ShiftRotationActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        if (state.rotations.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.tools_shifts_empty_title),
                message = stringResource(R.string.tools_shifts_empty_message),
            )
        }
        state.rotations.forEach { rotation -> RotationCard(rotation, actions) }
        Button(onClick = actions.onNew, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tools_shifts_new))
        }
    }
}

@Composable
private fun RotationCard(
    rotation: ShiftRotation,
    actions: ShiftRotationActions,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                rotation.pattern.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!rotation.isActive) {
                Text(stringResource(R.string.tools_shifts_inactive), style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { actions.onEdit(rotation) }) {
                    Text(stringResource(R.string.tools_shifts_edit))
                }
                TextButton(onClick = { actions.onDelete(rotation) }) {
                    Text(stringResource(R.string.tools_shifts_delete))
                }
            }
        }
    }
}

@Composable
private fun RotationEditor(
    draft: ShiftRotationDraft,
    actions: ShiftRotationActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = actions.onName,
            label = { Text(stringResource(R.string.tools_shifts_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ShiftSection(R.string.tools_shifts_types)
        ShiftTypes(draft, actions)
        ShiftSection(R.string.tools_shifts_pattern)
        PatternRow(draft, actions)
        ShiftSection(R.string.tools_shifts_anchor)
        TextButton(onClick = actions.onPickAnchor) { Text(draft.anchorText) }
        // The switch is the only control here with no text of its own, so it carries the row's label (T-1700).
        val activeLabel = stringResource(R.string.tools_shifts_active)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Text(activeLabel, Modifier.weight(1f))
            Switch(
                checked = draft.isActive,
                onCheckedChange = actions.onActive,
                modifier = Modifier.semantics { contentDescription = activeLabel },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Button(onClick = actions.onSave, enabled = draft.canSave) {
                Text(stringResource(R.string.tools_shifts_save))
            }
            TextButton(onClick = actions.onCancel) { Text(stringResource(R.string.tools_shifts_cancel)) }
        }
    }
}

@Composable
private fun ShiftTypes(
    draft: ShiftRotationDraft,
    actions: ShiftRotationActions,
) {
    // The colours get a row of their own: six swatches, a label and a remove button on one line left the button
    // 12 dp wide, which the accessibility audit caught as a touch target overlapping its neighbour (T-1700).
    draft.types.forEachIndexed { index, type ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Swatch(type.color)
                Text(type.label, Modifier.weight(1f).padding(horizontal = 8.dp))
                val removeLabel = stringResource(R.string.tools_shifts_type_remove_named, type.label)
                TextButton(
                    onClick = { actions.onRemoveType(index) },
                    modifier = Modifier.semantics { contentDescription = removeLabel },
                ) { Text(stringResource(R.string.tools_shifts_type_remove)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SHIFT_COLORS.forEachIndexed { colorIndex, color ->
                    val label =
                        stringResource(R.string.tools_shifts_type_color, (colorIndex + 1).toString(), type.label)
                    TextButton(
                        onClick = { actions.onTypeColor(index, color) },
                        modifier = Modifier.semantics { contentDescription = label },
                    ) { Swatch(color) }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft.newTypeLabel,
            onValueChange = actions.onNewTypeLabel,
            label = { Text(stringResource(R.string.tools_shifts_type_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = actions.onAddType) { Text(stringResource(R.string.tools_shifts_type_add)) }
    }
}

@Composable
private fun PatternRow(
    draft: ShiftRotationDraft,
    actions: ShiftRotationActions,
) {
    Text(
        draft.patternTypes.joinToString(" · ") { it.label }.ifEmpty {
            stringResource(R.string.tools_shifts_pattern_empty)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        draft.types.chunked(CHIPS_PER_ROW).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { columnIndex, type ->
                    val index = rowIndex * CHIPS_PER_ROW + columnIndex
                    FilterChip(
                        selected = false,
                        onClick = { actions.onAppendToPattern(index) },
                        label = { Text(type.label) },
                    )
                }
            }
        }
    }
    TextButton(onClick = actions.onRemoveLastFromPattern, enabled = draft.pattern.isNotEmpty()) {
        Text(stringResource(R.string.tools_shifts_pattern_undo))
    }
}

@Composable
private fun Swatch(color: Int?) {
    val fill = color?.let(::Color) ?: MaterialTheme.colorScheme.outline
    Box(Modifier.size(SWATCH).background(fill, CircleShape))
}

@Composable
private fun ShiftSection(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

private const val CHIPS_PER_ROW = 3
