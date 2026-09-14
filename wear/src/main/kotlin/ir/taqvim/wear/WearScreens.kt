/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import ir.taqvim.core.model.CalendarSystem

/** A scrolling watch screen with the standard scaffold. */
@Composable
internal fun WearList(content: TransformingLazyColumnScope.() -> Unit) {
    val state = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, content = content)
    }
}

/** Centered body text. */
@Composable
internal fun Centered(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/** A full-width button with [label]. */
@Composable
internal fun WideButton(
    label: String,
    onClick: () -> Unit,
    secondary: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        secondaryLabel = secondary?.let { { Text(it) } },
    ) { Text(label) }
}

/** Today (T-1600): the primary date, the other calendars, holidays and occasions, the next prayer and the menu. */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    WearList {
        val today = state.today
        if (today == null) {
            item { Centered(stringResource(R.string.wear_loading)) }
            return@WearList
        }
        item { ListHeader { Text(stringResource(R.string.wear_today)) } }
        item { Centered(today.primaryDate) }
        today.secondaryDates.forEach { date -> item { Centered(date) } }
        today.holidays.forEach { holiday ->
            item { Centered(stringResource(R.string.wear_event_line, holiday, stringResource(R.string.wear_holidays))) }
        }
        today.events.forEach { event -> item { Centered(event) } }
        item {
            val next = today.nextPrayer
            Centered(
                next?.let { stringResource(R.string.wear_next_prayer_at, context.prayerName(it.prayer), it.clock) }
                    ?: stringResource(R.string.wear_no_place),
            )
        }
        item { WideButton(stringResource(R.string.wear_month), { onOpen(WearRoutes.MONTH) }) }
        item { WideButton(stringResource(R.string.wear_converter), { onOpen(WearRoutes.CONVERTER) }) }
        item { WideButton(stringResource(R.string.wear_settings), { onOpen(WearRoutes.SETTINGS) }, state.placeName) }
    }
}

/** Month (T-1600): the primary-calendar month grid with today, holidays and weekends highlighted. */
@Composable
fun MonthScreen(
    state: MonthUiState,
    onShow: (Int) -> Unit,
) {
    WearList {
        val month = state.month
        if (month == null) {
            item { Centered(stringResource(R.string.wear_loading)) }
            return@WearList
        }
        item { ListHeader { Text(month.title) } }
        item { MonthGrid(month) }
        item { WideButton(stringResource(R.string.wear_month_next), { onShow(1) }) }
        item { WideButton(stringResource(R.string.wear_month_previous), { onShow(-1) }) }
        if (state.offset != 0) item { WideButton(stringResource(R.string.wear_month_current), { onShow(0) }) }
    }
}

@Composable
private fun MonthGrid(month: WearMonth) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        GridRow(month.weekdayLabels.map { it to MaterialTheme.colorScheme.onSurfaceVariant })
        month.weeks.forEach { week ->
            GridRow(
                week.map { cell ->
                    val color =
                        when {
                            cell == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            cell.isToday -> MaterialTheme.colorScheme.primary
                            cell.isHoliday || cell.isWeekend -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    (cell?.label.orEmpty()) to color
                },
            )
        }
    }
}

@Composable
private fun GridRow(cells: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.Center) {
        cells.forEach { (label, color) ->
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(CELL_WIDTH.dp),
            )
        }
    }
}

private const val CELL_WIDTH = 22

/** Converter (T-1600): step the year, month and day in one calendar and read the date in the others. */
@Composable
fun ConverterScreen(
    state: ConverterUiState,
    onStep: (ConverterField, Int) -> Unit,
    onSwitch: (CalendarSystem) -> Unit,
) {
    WearList {
        val source = state.source
        if (source == null) {
            item { Centered(stringResource(R.string.wear_loading)) }
            return@WearList
        }
        item { ListHeader { Text(stringResource(R.string.wear_converter)) } }
        item { Centered(state.sourceText) }
        ConverterField.entries.forEach { field -> item { StepperRow(field, onStep) } }
        item { ListHeader { Text(stringResource(R.string.wear_other_calendars)) } }
        state.results.forEach { result ->
            item { WideButton(result.text, { onSwitch(result.system) }, stringResource(calendarLabel(result.system))) }
        }
    }
}

@Composable
private fun StepperRow(
    field: ConverterField,
    onStep: (ConverterField, Int) -> Unit,
) {
    val name = stringResource(fieldLabel(field))
    val decrease = stringResource(R.string.wear_converter_decrease, name)
    val increase = stringResource(R.string.wear_converter_increase, name)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Button(onClick = { onStep(field, -1) }, modifier = Modifier.semantics { contentDescription = decrease }) {
            Text(stringResource(R.string.wear_symbol_minus))
        }
        Text(text = name, modifier = Modifier.width(STEPPER_LABEL_WIDTH.dp), textAlign = TextAlign.Center)
        Button(onClick = { onStep(field, 1) }, modifier = Modifier.semantics { contentDescription = increase }) {
            Text(stringResource(R.string.wear_symbol_plus))
        }
    }
}

private const val STEPPER_LABEL_WIDTH = 48
