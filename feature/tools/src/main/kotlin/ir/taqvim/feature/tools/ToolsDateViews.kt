/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The date converter: a phrase or date in, the same day in every calendar out. */
@Composable
internal fun ConverterTool(
    input: String,
    result: ConverterResult,
    onInput: (String) -> Unit,
) {
    val hint = stringResource(R.string.tools_converter_hint)
    ToolField(input, R.string.tools_converter_input, onInput, supporting = hint)
    when (result) {
        ConverterResult.NotRecognized -> {
            ToolMessage(stringResource(R.string.tools_not_recognized), isError = true)
        }

        is ConverterResult.Converted -> {
            if (result.isToday) {
                Text(stringResource(R.string.tools_converter_today), style = MaterialTheme.typography.titleSmall)
            }
            result.dates.forEach { DateCard(it) }
        }
    }
}

@Composable
private fun DateCard(date: ConvertedDate) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(date.system.label), style = MaterialTheme.typography.labelLarge)
            Text(date.long, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.tools_date_numeric_iso, date.numeric, date.iso),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The distance between two dates: days, weeks, years/months/days and workdays. */
@Composable
internal fun DistanceTool(
    inputs: ToolsInputs,
    state: DistanceState,
    onInputsChange: (ToolsInputs) -> Unit,
) {
    DistanceField(inputs.distanceFrom, R.string.tools_distance_from, state.from) {
        onInputsChange(inputs.copy(distanceFrom = it))
    }
    DistanceField(inputs.distanceTo, R.string.tools_distance_to, state.to) {
        onInputsChange(inputs.copy(distanceTo = it))
    }
    state.result?.let { DistanceResultView(it) }
}

@Composable
private fun DistanceField(
    value: String,
    @StringRes label: Int,
    end: DistanceEnd,
    onValueChange: (String) -> Unit,
) {
    val supporting =
        when (end) {
            DistanceEnd.Empty -> stringResource(R.string.tools_converter_hint)
            DistanceEnd.NotRecognized -> stringResource(R.string.tools_not_recognized)
            is DistanceEnd.Recognized -> end.date
        }
    ToolField(value, label, onValueChange, supporting = supporting, isError = end == DistanceEnd.NotRecognized)
}

@Composable
private fun DistanceResultView(result: DistanceResult) {
    val calendarName = stringResource(result.calendar.label)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result.isBackward) ToolMessage(stringResource(R.string.tools_distance_backward))
            result.daysText?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            LabeledValue(stringResource(R.string.tools_distance_days), result.days)
            LabeledValue(
                stringResource(R.string.tools_distance_weeks),
                stringResource(R.string.tools_value_plus, result.weeks, result.weekDays),
            )
            Text(
                stringResource(R.string.tools_distance_period, calendarName),
                style = MaterialTheme.typography.titleSmall,
            )
            LabeledValue(stringResource(R.string.tools_distance_years), result.years)
            LabeledValue(stringResource(R.string.tools_distance_months), result.months)
            LabeledValue(stringResource(R.string.tools_distance_days), result.monthDays)
            when (val workdays = result.workdays) {
                WorkdaysText.NotConfigured -> ToolMessage(stringResource(R.string.tools_workdays_not_configured))
                WorkdaysText.TooLong -> ToolMessage(stringResource(R.string.tools_workdays_too_long))
                is WorkdaysText.Count -> LabeledValue(stringResource(R.string.tools_distance_workdays), workdays.value)
            }
        }
    }
}
