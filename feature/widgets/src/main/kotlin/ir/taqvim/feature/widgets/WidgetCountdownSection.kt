/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerModel
import ir.taqvim.core.ui.component.DatePickerSheet
import ir.taqvim.core.ui.component.DateSelection

/** The countdown widget's date (T-1212, F-09): until or since, a title, an occasion or a date in a calendar. */
@Composable
internal fun CountdownSection(
    choices: WidgetCountdownChoices,
    countdown: WidgetCountdown,
    actions: WidgetCountdownActions,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    SectionTitle(R.string.widget_countdown_config_title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CountdownMode.entries.forEach { mode ->
            FilterChip(
                selected = countdown.mode == mode,
                onClick = { actions.onMode(mode) },
                label = { Text(stringResource(mode.label)) },
            )
        }
    }
    OutlinedTextField(
        value = countdown.title,
        onValueChange = actions.onTitle,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.widget_countdown_config_label)) },
        singleLine = true,
    )
    OccasionChoices(choices, countdown, actions)
    DateChoices(choices, countdown, actions) { picking = true }
    if (picking) CountdownDatePicker(choices, countdown, actions) { picking = false }
}

/** The calendar, the chosen date with its picker button, and whether it repeats every year. */
@Composable
private fun DateChoices(
    choices: WidgetCountdownChoices,
    countdown: WidgetCountdown,
    actions: WidgetCountdownActions,
    onChangeDate: () -> Unit,
) {
    SectionTitle(R.string.widget_countdown_config_calendar)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.calendars.forEach { calendar ->
            FilterChip(
                selected = countdown.calendar == calendar,
                onClick = { actions.onCalendar(calendar) },
                label = { Text(stringResource(calendarLabel(calendar))) },
            )
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(choices.dateText, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onChangeDate) { Text(stringResource(R.string.widget_countdown_config_change_date)) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = countdown.repeatsYearly, role = Role.Switch, onValueChange = actions.onRepeats)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.widget_countdown_config_repeats),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = countdown.repeatsYearly, onCheckedChange = null)
    }
}

@Composable
private fun OccasionChoices(
    choices: WidgetCountdownChoices,
    countdown: WidgetCountdown,
    actions: WidgetCountdownActions,
) {
    if (choices.occasions.isEmpty()) return
    SectionTitle(R.string.widget_countdown_config_occasions)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.occasions.forEach { occasion ->
            val selected =
                countdown.title == occasion.title &&
                    countdown.calendar == occasion.calendar &&
                    countdown.month == occasion.month &&
                    countdown.day == occasion.day
            FilterChip(
                selected = selected,
                onClick = { actions.onOccasion(occasion) },
                label = { Text(stringResource(R.string.widget_countdown_occasion, occasion.title, occasion.dateText)) },
            )
        }
    }
}

@Composable
private fun CountdownDatePicker(
    choices: WidgetCountdownChoices,
    countdown: WidgetCountdown,
    actions: WidgetCountdownActions,
    onClose: () -> Unit,
) {
    val labels =
        DatePickerLabels(
            title = stringResource(R.string.widget_countdown_picker_title),
            year = stringResource(R.string.widget_countdown_picker_year),
            month = stringResource(R.string.widget_countdown_picker_month),
            day = stringResource(R.string.widget_countdown_picker_day),
            confirm = stringResource(R.string.widget_countdown_picker_confirm),
            cancel = stringResource(R.string.widget_config_cancel),
        )
    val year = countdown.year.coerceIn(choices.years.first, choices.years.last)
    val month = countdown.month.coerceIn(1, choices.monthNames.size)
    val model =
        DatePickerModel(
            initial = DateSelection(year, month, countdown.day.coerceIn(1, actions.daysInMonth(year, month))),
            years = choices.years,
            monthNames = choices.monthNames,
            daysInMonth = actions.daysInMonth,
            formatNumber = { Numerals.localizeDigits(it.toString(), choices.numerals) },
            labels = labels,
        )
    DatePickerSheet(
        model = model,
        onConfirm = { selection ->
            actions.onDate(selection.year, selection.month, selection.day)
            onClose()
        },
        onDismiss = onClose,
    )
}

/** String resource of a countdown mode. */
@get:StringRes
internal val CountdownMode.label: Int
    get() =
        when (this) {
            CountdownMode.UNTIL -> R.string.widget_countdown_mode_until
            CountdownMode.SINCE -> R.string.widget_countdown_mode_since
        }
