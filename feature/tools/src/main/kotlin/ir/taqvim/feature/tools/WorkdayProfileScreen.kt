/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.R as SharedR
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerSheet
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import ir.taqvim.core.workdays.HalfDayPolicy

private val GAP = 12.dp
private val PADDING = 16.dp

/**
 * The workday profiles screen (F-07), stateless.
 *
 * Several named profiles, because one person can work to more than one calendar — an office week in Iran, a client's
 * week elsewhere. Exactly one is the default, and that is the one the workday calculator uses; the rest sit ready.
 */
@Composable
fun WorkdayProfileScreen(
    state: WorkdayProfileUiState,
    actions: WorkdayProfileActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.tools_profiles_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Loading()
                state.editing != null -> ProfileEditor(state.editing, actions)
                else -> ProfileList(state, actions)
            }
            state.picker?.let { picker ->
                DatePickerSheet(
                    model = picker.model(pickerLabels(state.pickingLeave)),
                    onConfirm = actions.onLeaveDatePicked,
                    onDismiss = actions.onDismissPicker,
                )
            }
        }
    }
}

@Composable
private fun Loading() {
    val description = stringResource(R.string.tools_profiles_loading)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** The labels of the leave picker; the title says which end of the range is being chosen. */
@Composable
private fun pickerLabels(bound: LeaveBound?): DatePickerLabels =
    DatePickerLabels(
        title =
            stringResource(
                if (bound ==
                    LeaveBound.LAST
                ) {
                    R.string.tools_profiles_leave_last
                } else {
                    R.string.tools_profiles_leave_first
                },
            ),
        year = stringResource(SharedR.string.shared_picker_year),
        month = stringResource(SharedR.string.shared_picker_month),
        day = stringResource(SharedR.string.shared_picker_day),
        confirm = stringResource(SharedR.string.shared_picker_confirm),
        cancel = stringResource(SharedR.string.shared_picker_cancel),
    )

@Composable
private fun ProfileList(
    state: WorkdayProfileUiState,
    actions: WorkdayProfileActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        if (state.profiles.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.tools_profiles_empty_title),
                message = stringResource(R.string.tools_profiles_empty_message),
            )
        }
        state.profiles.forEach { stored -> ProfileCard(stored, actions) }
        Button(onClick = actions.onNew, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tools_profiles_new))
        }
    }
}

@Composable
private fun ProfileCard(
    stored: NamedWorkdayProfile,
    actions: WorkdayProfileActions,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stored.name, style = MaterialTheme.typography.titleMedium)
            if (stored.isDefault) {
                Text(stringResource(R.string.tools_profiles_is_default), style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { actions.onEdit(stored) }) {
                    Text(stringResource(R.string.tools_profiles_edit))
                }
                if (!stored.isDefault) {
                    TextButton(onClick = { actions.onMakeDefault(stored) }) {
                        Text(stringResource(R.string.tools_profiles_make_default))
                    }
                }
                TextButton(onClick = { actions.onDelete(stored) }) {
                    Text(stringResource(R.string.tools_profiles_delete))
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    draft: WorkdayProfileDraft,
    actions: WorkdayProfileActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = actions.onName,
            label = { Text(stringResource(R.string.tools_profiles_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Section(R.string.tools_profiles_weekend)
        ChipRow(Weekday.entries, draft.weekend, { weekdayLabel(it) }) { day, on -> actions.onWeekend(day, on) }
        Section(R.string.tools_profiles_holiday_sources)
        ChipRow(SELECTABLE_SOURCES, draft.holidaySources, { sourceLabel(it) }) { source, on ->
            actions.onHolidaySource(source, on)
        }
        Section(R.string.tools_profiles_half_days)
        ChipRow(HalfDayPolicy.entries, setOf(draft.halfDays), { halfDayLabel(it) }) { policy, _ ->
            actions.onHalfDays(policy)
        }
        Section(R.string.tools_profiles_leave)
        draft.leave.forEach { row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(row.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { actions.onRemoveLeave(row) }) {
                    Text(stringResource(R.string.tools_profiles_leave_remove))
                }
            }
        }
        TextButton(onClick = actions.onAddLeave) { Text(stringResource(R.string.tools_profiles_leave_add)) }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Button(onClick = actions.onSave, enabled = draft.canSave) {
                Text(stringResource(R.string.tools_profiles_save))
            }
            TextButton(onClick = actions.onCancel) { Text(stringResource(R.string.tools_profiles_cancel)) }
        }
    }
}

@Composable
private fun Section(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

/** One toggle chip per [items]; [selected] are the ones that are on. */
@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: Set<T>,
    label: @Composable (T) -> String,
    onChange: (T, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(CHIPS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { item ->
                    val on = item in selected
                    FilterChip(selected = on, onClick = { onChange(item, !on) }, label = { Text(label(item)) })
                }
            }
        }
    }
}

private const val CHIPS_PER_ROW = 3

/** The sources whose holidays can count as days off; a user's own events are not a dataset source. */
private val SELECTABLE_SOURCES: List<EventSource> = EventSource.entries - EventSource.USER

@Composable
private fun weekdayLabel(day: Weekday): String = stringResource(ToolsLabels.weekday(day))

@Composable
private fun sourceLabel(source: EventSource): String = stringResource(ToolsLabels.source(source))

@Composable
private fun halfDayLabel(policy: HalfDayPolicy): String = stringResource(ToolsLabels.halfDay(policy))
