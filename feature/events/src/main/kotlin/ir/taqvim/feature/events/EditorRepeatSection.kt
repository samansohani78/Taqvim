/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ui.component.SegmentedTabs

private val FREQUENCIES: List<Frequency?> = listOf(null) + Frequency.entries

/** Repetition (T-503 rules in the event's calendar, ADR-0011). */
@Composable
internal fun RepeatSection(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
    onPick: (PickerTarget) -> Unit,
) {
    val repeat = editing.form.repeat
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.events_repeat_label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().selectableGroup(),
        ) {
            FREQUENCIES.forEach { frequency ->
                FilterChip(
                    selected = frequency == repeat?.frequency,
                    onClick = { onIntent(SetRepeat(frequency)) },
                    label = { Text(stringResource(frequencyLabel(frequency))) },
                )
            }
        }
        if (repeat != null) {
            IntervalField(repeat, editing.errors, onIntent)
            if (repeat.frequency == Frequency.WEEKLY) WeekdayChips(editing.display, onIntent)
            if (repeat.frequency == Frequency.MONTHLY || repeat.frequency == Frequency.YEARLY) {
                InvalidDaysChoice(repeat.invalidDates, onIntent)
            }
            RepeatEnd(editing, repeat, onIntent, onPick)
            RepeatPreview(editing.display.preview)
        }
    }
}

/** The next occurrences of the series (review F02), so leap days, weekdays and time zones can be checked. */
@Composable
private fun RepeatPreview(preview: List<PreviewOccurrence>) {
    if (preview.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().testTag(REPEAT_PREVIEW_TAG),
    ) {
        Text(stringResource(R.string.events_repeat_preview_label))
        preview.forEach { occurrence ->
            val text =
                occurrence.time?.let { stringResource(R.string.events_repeat_preview_timed, occurrence.date, it) }
                    ?: occurrence.date
            Text(text)
        }
    }
}

/** Test tag of the list of upcoming occurrences. */
const val REPEAT_PREVIEW_TAG: String = "events_repeat_preview"

@Composable
private fun IntervalField(
    repeat: RepeatForm,
    errors: Set<EditorError>,
    onIntent: (EditorIntent) -> Unit,
) {
    val error = firstError(errors, EditorError.INTERVAL_INVALID)
    OutlinedTextField(
        value = repeat.intervalText,
        onValueChange = { onIntent(RepeatIntent.Interval(it)) },
        label = { Text(stringResource(intervalLabel(repeat.frequency))) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WeekdayChips(
    display: EditorDisplay,
    onIntent: (EditorIntent) -> Unit,
) {
    Text(stringResource(R.string.events_repeat_on_days))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        display.weekdays.forEach { option ->
            FilterChip(
                selected = option.selected,
                onClick = { onIntent(RepeatIntent.ToggleWeekday(option.weekday)) },
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
private fun InvalidDaysChoice(
    selected: InvalidDatePolicy,
    onIntent: (EditorIntent) -> Unit,
) {
    Text(stringResource(R.string.events_invalid_days_label))
    Column(Modifier.selectableGroup()) {
        InvalidDatePolicy.entries.forEach { policy ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = policy == selected,
                            role = Role.RadioButton,
                            onClick = { onIntent(RepeatIntent.InvalidDays(policy)) },
                        ).padding(vertical = 4.dp),
            ) {
                RadioButton(selected = policy == selected, onClick = null)
                Text(stringResource(policyLabel(policy)), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun RepeatEnd(
    editing: EditorContent.Editing,
    repeat: RepeatForm,
    onIntent: (EditorIntent) -> Unit,
    onPick: (PickerTarget) -> Unit,
) {
    Text(stringResource(R.string.events_repeat_end_label))
    SegmentedTabs(
        tabs = RecurrenceEnd.entries.map { stringResource(endLabel(it)) },
        selectedIndex = repeat.end.ordinal,
        onSelect = { onIntent(RepeatIntent.Ends(RecurrenceEnd.entries[it])) },
    )
    if (repeat.end == RecurrenceEnd.COUNT) {
        val error = firstError(editing.errors, EditorError.COUNT_INVALID)
        OutlinedTextField(
            value = repeat.countText,
            onValueChange = { onIntent(RepeatIntent.Count(it)) },
            label = { Text(stringResource(R.string.events_repeat_count_label)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (repeat.end == RecurrenceEnd.UNTIL) {
        val until = editing.display.untilDate.orEmpty()
        val description = stringResource(R.string.events_until_description, until)
        OutlinedButton(
            onClick = { onPick(PickerTarget.UNTIL_DATE) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        ) { Text(until) }
        firstError(editing.errors, EditorError.UNTIL_BEFORE_START)?.let { ErrorLine(it) }
    }
}
