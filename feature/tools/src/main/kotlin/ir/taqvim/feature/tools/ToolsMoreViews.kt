/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The duration calculator: an expression in, its parts and totals out. */
@Composable
internal fun DurationTool(
    input: String,
    state: DurationState,
    onInput: (String) -> Unit,
) {
    val error = (state as? DurationState.Invalid)?.error
    ToolField(
        input,
        R.string.tools_duration_input,
        onInput,
        supporting = error?.let { durationErrorText(it) } ?: stringResource(R.string.tools_duration_hint),
        isError = error != null,
        leftToRight = true,
    )
    if (state is DurationState.Value) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isNegative) ToolMessage(stringResource(R.string.tools_duration_negative))
                state.text?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
                LabeledValue(stringResource(R.string.tools_distance_days), state.days)
                LabeledValue(stringResource(R.string.tools_duration_hours), state.hours)
                LabeledValue(stringResource(R.string.tools_duration_minutes), state.minutes)
                LabeledValue(stringResource(R.string.tools_duration_seconds), state.seconds)
                LabeledValue(stringResource(R.string.tools_duration_total_hours), state.totalHours)
                LabeledValue(stringResource(R.string.tools_duration_total_minutes), state.totalMinutes)
            }
        }
    }
}

@Composable
private fun durationErrorText(error: DurationError): String {
    val position = error.position + 1
    return when (error) {
        is DurationError.UnexpectedCharacter -> stringResource(R.string.tools_duration_error_character, position)
        is DurationError.UnexpectedToken -> stringResource(R.string.tools_duration_error_token, position)
        is DurationError.MissingUnit -> stringResource(R.string.tools_duration_error_unit, position)
        is DurationError.UnbalancedParenthesis -> stringResource(R.string.tools_duration_error_parenthesis, position)
        is DurationError.DivisionByZero -> stringResource(R.string.tools_duration_error_division, position)
        is DurationError.TooLarge -> stringResource(R.string.tools_duration_error_large)
        is DurationError.TooComplex -> stringResource(R.string.tools_duration_error_complex)
    }
}

/** The time-zone board with its zone search. */
@Composable
internal fun TimeZonesTool(
    inputs: ToolsInputs,
    board: TimeZoneBoard,
    actions: ToolsActions,
) {
    ToolField(inputs.zoneQuery, R.string.tools_zone_search, { actions.onInputsChange(inputs.copy(zoneQuery = it)) })
    board.suggestions.forEach { id ->
        TextButton(onClick = { actions.onAddZone(id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tools_zone_add, id))
        }
    }
    board.rows.forEach { ZoneCard(it, onRemove = { actions.onRemoveZone(it.id) }) }
}

@Composable
private fun ZoneCard(
    row: ZoneRow,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(row.city, style = MaterialTheme.typography.titleMedium)
                Text(row.name, style = MaterialTheme.typography.bodySmall)
                val details =
                    listOfNotNull(
                        stringResource(R.string.tools_zone_offset, row.offset),
                        dayShiftText(row.dayShift),
                        stringResource(R.string.tools_zone_home).takeIf { row.isHome },
                    )
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.time, style = MaterialTheme.typography.headlineMedium)
                if (!row.isHome) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.tools_zone_remove, row.city)) }
                }
            }
        }
    }
}

@Composable
private fun dayShiftText(shift: Int): String? =
    when {
        shift == 0 -> null
        shift == -1 -> stringResource(R.string.tools_zone_previous_day)
        shift == 1 -> stringResource(R.string.tools_zone_next_day)
        shift > 0 -> stringResource(R.string.tools_zone_days_later, shift.toString())
        else -> stringResource(R.string.tools_zone_days_earlier, (-shift).toString())
    }

/** The QR generator: text in, a QR code to look at or share out. */
@Composable
internal fun QrTool(
    input: String,
    state: QrState,
    actions: ToolsActions,
    onInput: (String) -> Unit,
) {
    ToolField(input, R.string.tools_qr_input, onInput, singleLine = false)
    when (state) {
        QrState.Empty -> {
            ToolMessage(stringResource(R.string.tools_qr_empty))
        }

        QrState.TooLong -> {
            ToolMessage(stringResource(R.string.tools_qr_too_long), isError = true)
        }

        is QrState.Code -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { QrImage(state.matrix) }
            Button(onClick = actions.onShareQr, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tools_qr_share))
            }
        }
    }
}

/** Black modules on a white square with a four-module quiet zone, in every theme so scanners can read it. */
@Composable
private fun QrImage(
    matrix: QrMatrix,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.tools_qr_image)
    Canvas(
        modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.White)
            .semantics { contentDescription = description },
    ) {
        val modules = matrix.size + 2 * QUIET_ZONE_MODULES
        val cell = size.minDimension / modules
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset((x + QUIET_ZONE_MODULES) * cell, (y + QUIET_ZONE_MODULES) * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

private const val QUIET_ZONE_MODULES = 4
