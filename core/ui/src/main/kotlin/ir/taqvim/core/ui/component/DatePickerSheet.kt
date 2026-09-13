/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A year/month/day triple in any calendar; months and days are 1-based. */
@Immutable
public data class DateSelection(
    public val year: Int,
    public val month: Int,
    public val day: Int,
) {
    /** This selection in [year], with the day clamped to that month's length. */
    public fun withYear(
        year: Int,
        daysInMonth: (year: Int, month: Int) -> Int,
    ): DateSelection = copy(year = year).clampDay(daysInMonth)

    /** This selection in [month], with the day clamped to that month's length. */
    public fun withMonth(
        month: Int,
        daysInMonth: (year: Int, month: Int) -> Int,
    ): DateSelection = copy(month = month).clampDay(daysInMonth)

    /** This selection on [day], clamped to the month's length. */
    public fun withDay(
        day: Int,
        daysInMonth: (year: Int, month: Int) -> Int,
    ): DateSelection = copy(day = day).clampDay(daysInMonth)

    private fun clampDay(daysInMonth: (year: Int, month: Int) -> Int): DateSelection =
        copy(day = day.coerceIn(1, daysInMonth(year, month).coerceAtLeast(1)))
}

/** Texts of a [DatePickerSheet]; the wheel labels name each wheel for accessibility services. */
@Immutable
public data class DatePickerLabels(
    public val title: String,
    public val year: String,
    public val month: String,
    public val day: String,
    public val confirm: String,
    public val cancel: String,
)

/**
 * What a [DatePickerSheet] offers: the [initial] date, selectable [years], the calendar's [monthNames] (month 1 first)
 * and [daysInMonth], and [formatNumber] for localized digits. The picker is calendar-agnostic.
 */
@Immutable
public data class DatePickerModel(
    public val initial: DateSelection,
    public val years: IntRange,
    public val monthNames: List<String>,
    public val daysInMonth: (year: Int, month: Int) -> Int,
    public val formatNumber: (Int) -> String,
    public val labels: DatePickerLabels,
) {
    init {
        require(monthNames.isNotEmpty()) { "A calendar needs month names" }
        require(!years.isEmpty()) { "The year range must not be empty" }
    }

    /** Name of 1-based [month], clamped to the known months. */
    public fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, monthNames.size - 1)]
}

/** The body of [DatePickerSheet]: title, day/month/year wheels and cancel/confirm buttons. */
@Composable
public fun DatePickerContent(
    model: DatePickerModel,
    onConfirm: (DateSelection) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember(model) { mutableStateOf(model.initial.withDay(model.initial.day, model.daysInMonth)) }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            model.labels.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val days = 1..model.daysInMonth(selection.year, selection.month).coerceAtLeast(1)
            NumberWheel(
                selection.day,
                days,
                { selection = selection.withDay(it, model.daysInMonth) },
                model.labels.day,
                Modifier.weight(1f),
                model.formatNumber,
            )
            NumberWheel(
                selection.month,
                1..model.monthNames.size,
                { selection = selection.withMonth(it, model.daysInMonth) },
                model.labels.month,
                Modifier.weight(1.6f),
                model::monthName,
            )
            NumberWheel(
                selection.year,
                model.years,
                { selection = selection.withYear(it, model.daysInMonth) },
                model.labels.year,
                Modifier.weight(1.2f),
                model.formatNumber,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(model.labels.cancel) }
            TextButton(onClick = { onConfirm(selection) }) { Text(model.labels.confirm) }
        }
    }
}

/** A modal bottom sheet for picking a date in any calendar (T-701); [onConfirm] receives the chosen date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DatePickerSheet(
    model: DatePickerModel,
    onConfirm: (DateSelection) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        DatePickerContent(model, onConfirm, onDismiss)
    }
}
