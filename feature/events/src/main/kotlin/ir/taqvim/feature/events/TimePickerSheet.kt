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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.NumberWheel

private const val MINUTES_PER_HOUR = 60
private val HOURS = 0..23
private val MINUTES = 0..59

/** A bottom sheet with hour and minute wheels; [onConfirm] receives the minute of the day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerSheet(
    title: String,
    initialMinute: Int,
    picker: CalendarPickerData,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        TimePickerContent(title, initialMinute, picker, onDismiss, onConfirm)
    }
}

/** The body of [TimePickerSheet]. */
@Composable
internal fun TimePickerContent(
    title: String,
    initialMinute: Int,
    picker: CalendarPickerData,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by remember(initialMinute) { mutableIntStateOf(initialMinute / MINUTES_PER_HOUR) }
    var minute by remember(initialMinute) { mutableIntStateOf(initialMinute % MINUTES_PER_HOUR) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberWheel(
                hour,
                HOURS,
                { hour = it },
                stringResource(R.string.events_picker_hour),
                Modifier.weight(1f),
                picker::formatTwoDigits,
            )
            NumberWheel(
                minute,
                MINUTES,
                { minute = it },
                stringResource(R.string.events_picker_minute),
                Modifier.weight(1f),
                picker::formatTwoDigits,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_picker_cancel)) }
            TextButton(onClick = { onConfirm(hour * MINUTES_PER_HOUR + minute) }) {
                Text(stringResource(R.string.events_picker_confirm))
            }
        }
    }
}
