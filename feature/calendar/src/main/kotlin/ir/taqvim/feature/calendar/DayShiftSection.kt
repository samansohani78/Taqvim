/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * The shift worked on the selected day (F-08), and the way to correct it.
 *
 * A pattern is what usually happens; the exception is what did. Both live on the day itself, because that is where a
 * user is standing when they find out they are covering someone else's night.
 */
@Composable
internal fun ShiftOnThisDay(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
) {
    val rotation = content.shiftRotations.firstOrNull { it.isActive } ?: return
    val day = content.selectedDay
    val current = rotation.shiftOn(day)
    val isException = rotation.exceptions.containsKey(day.value)
    DetailRow(
        stringResource(R.string.calendar_shift_exception),
        current?.label ?: stringResource(R.string.calendar_shift_none),
        note = rotation.name,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        rotation.types.forEach { type ->
            FilterChip(
                selected = type.label == current?.label,
                onClick = { onAction(CalendarAction.SetShiftException(rotation.id, type.label)) },
                label = { Text(type.label) },
            )
        }
    }
    if (isException) {
        TextButton(onClick = { onAction(CalendarAction.SetShiftException(rotation.id, null)) }) {
            Text(stringResource(R.string.calendar_shift_clear))
        }
    }
}

/**
 * Writes the shift actually worked on the selected day (F-08), which the rotation's pattern alone cannot know.
 *
 * A top-level helper rather than a method so [CalendarViewModel] stays inside the 400-line limit; it has no state of
 * its own, which is why it can be one.
 */
internal fun applyShiftException(
    viewModel: CalendarViewModel,
    shifts: ShiftScheduleSource,
    action: CalendarAction.SetShiftException,
) {
    val day =
        viewModel.uiState.value.content
            ?.selectedDay ?: return
    viewModel.viewModelScope.launch { shifts.setException(action.rotationId, day, action.shift) }
}
