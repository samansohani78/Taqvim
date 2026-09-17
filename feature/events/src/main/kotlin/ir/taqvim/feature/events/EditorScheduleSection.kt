/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerModel
import ir.taqvim.core.ui.component.DatePickerSheet
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.ui.component.SegmentedTabs

/** Calendar, all-day switch, start and end (T-1000). */
@Composable
internal fun ScheduleSection(
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
    onPick: (PickerTarget) -> Unit,
) {
    val form = editing.form
    val display = editing.display
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.events_calendar_label)
        SegmentedTabs(
            tabs = display.calendars.map { stringResource(calendarLabel(it)) },
            selectedIndex = display.calendars.indexOf(form.calendar),
            onSelect = { onIntent(ScheduleIntent.ChangeCalendar(display.calendars[it])) },
        )
        AllDaySwitch(form.allDay) { onIntent(ScheduleIntent.AllDay(it)) }
        DateTimeRow(
            label = stringResource(R.string.events_starts),
            date = stringResource(R.string.events_start_date_description, display.startDate) to display.startDate,
            time = stringResource(R.string.events_start_time_description, display.startTime) to display.startTime,
            allDay = form.allDay,
            onDate = { onPick(PickerTarget.START_DATE) },
            onTime = { onPick(PickerTarget.START_TIME) },
        )
        DateTimeRow(
            label = stringResource(R.string.events_ends),
            date = stringResource(R.string.events_end_date_description, display.endDate) to display.endDate,
            time = stringResource(R.string.events_end_time_description, display.endTime) to display.endTime,
            allDay = form.allDay,
            onDate = { onPick(PickerTarget.END_DATE) },
            onTime = { onPick(PickerTarget.END_TIME) },
        )
        firstError(editing.errors, EditorError.END_BEFORE_START)?.let { ErrorLine(it) }
    }
}

@Composable
private fun AllDaySwitch(
    allDay: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = allDay, role = Role.Switch, onValueChange = onChange)
                .padding(vertical = 4.dp),
    ) {
        Text(stringResource(R.string.events_all_day), Modifier.weight(1f))
        Switch(checked = allDay, onCheckedChange = null)
    }
}

/** A labelled date button and, unless [allDay], a time button; each pair is (description, visible text). */
@Composable
private fun DateTimeRow(
    label: String,
    date: Pair<String, String>,
    time: Pair<String, String>,
    allDay: Boolean,
    onDate: () -> Unit,
    onTime: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onDate,
                modifier = Modifier.weight(1f).semantics { contentDescription = date.first },
            ) {
                Text(date.second, textAlign = TextAlign.Center)
            }
            if (!allDay) {
                OutlinedButton(onClick = onTime, modifier = Modifier.semantics { contentDescription = time.first }) {
                    Text(time.second, maxLines = 1)
                }
            }
        }
    }
}

/** An error message announced politely. */
@Composable
internal fun ErrorLine(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** The open date or time picker for [target]. */
@Composable
internal fun EditorPicker(
    target: PickerTarget,
    editing: EditorContent.Editing,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
) {
    val form = editing.form
    val picker = editing.display.picker
    val chosen: (EditorIntent) -> Unit = {
        onClose()
        onIntent(it)
    }
    when (target) {
        PickerTarget.START_DATE -> {
            DateSheet(
                R.string.events_pick_start_date,
                form.start,
                picker,
                onClose,
            ) { chosen(ScheduleIntent.StartDate(it)) }
        }

        PickerTarget.END_DATE -> {
            DateSheet(R.string.events_pick_end_date, form.end, picker, onClose) { chosen(ScheduleIntent.EndDate(it)) }
        }

        PickerTarget.UNTIL_DATE -> {
            val until = form.repeat?.until ?: form.start
            DateSheet(R.string.events_pick_until, until, picker, onClose) { chosen(RepeatIntent.Until(it)) }
        }

        PickerTarget.START_TIME -> {
            TimePickerSheet(stringResource(R.string.events_pick_start_time), form.startMinute, picker, onClose) {
                chosen(ScheduleIntent.StartTime(it))
            }
        }

        PickerTarget.END_TIME -> {
            TimePickerSheet(stringResource(R.string.events_pick_end_time), form.endMinute, picker, onClose) {
                chosen(ScheduleIntent.EndTime(it))
            }
        }
    }
}

@Composable
private fun DateSheet(
    title: Int,
    date: CalendarDate,
    picker: CalendarPickerData,
    onDismiss: () -> Unit,
    onConfirm: (DateSelection) -> Unit,
) {
    val labels =
        DatePickerLabels(
            title = stringResource(title),
            year = stringResource(R.string.events_picker_year),
            month = stringResource(R.string.events_picker_month),
            day = stringResource(R.string.events_picker_day),
            confirm = stringResource(R.string.events_picker_confirm),
            cancel = stringResource(R.string.events_picker_cancel),
        )
    val model =
        DatePickerModel(
            initial = DateSelection(date.year, date.month, date.day),
            years = picker.years,
            monthNames = picker.monthNames,
            daysInMonth = picker::daysInMonth,
            formatNumber = picker::formatNumber,
            labels = labels,
            monthNamesIn = picker.monthNamesIn,
        )
    DatePickerSheet(model, onConfirm, onDismiss)
}
