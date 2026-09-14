/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EventChip
import ir.taqvim.core.ui.component.EventChipModel
import ir.taqvim.core.ui.motion.SharedKey
import ir.taqvim.core.ui.motion.WithSharedBounds

/** The list texts in the current configuration. */
@Composable
internal fun agendaTexts(): AgendaTexts {
    val monthTitle = stringResource(R.string.agenda_month_title)
    val monthRange = stringResource(R.string.agenda_month_range)
    val exportTitle = stringResource(R.string.agenda_export_title)
    return AgendaTexts(
        monthTitle = { name, year -> monthTitle.format(name, year) },
        monthRange = { first, last -> monthRange.format(first, last) },
        separator = stringResource(R.string.agenda_separator),
        today = stringResource(R.string.agenda_today),
        holiday = stringResource(R.string.agenda_holiday),
        noEvents = stringResource(R.string.agenda_no_events),
        emptyMonth = stringResource(R.string.agenda_empty_month),
        kinds =
            mapOf(
                AgendaEventKind.OFFICIAL to stringResource(R.string.agenda_kind_official),
                AgendaEventKind.PERSONAL to stringResource(R.string.agenda_kind_personal),
                AgendaEventKind.DEVICE to stringResource(R.string.agenda_kind_device),
                AgendaEventKind.SUBSCRIPTION to stringResource(R.string.agenda_kind_subscription),
            ),
        exportTitle = { from, to -> exportTitle.format(from, to) },
    )
}

@Composable
internal fun MonthHeaderRow(
    header: AgendaMonthHeader,
    texts: AgendaTexts,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            texts.title(header),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        texts.subtitle(header)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun EmptyMonthRow(texts: AgendaTexts) {
    Text(
        texts.emptyMonth,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(16.dp),
    )
}

@Composable
internal fun DayRow(
    day: AgendaDayRow,
    texts: AgendaTexts,
    onAction: (AgendaAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = if (day.isToday) colors.primaryContainer else Color.Transparent
    val accent = if (day.isHoliday || day.isWeekend) colors.error else colors.onSurface
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(background)
                .semantics { selected = day.isToday }
                .clickable { onAction(AgendaAction.OpenDay(day.jdn)) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                day.dayNumber,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayTitle(day, texts, accent)
                DayEvents(day, texts, onAction)
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun DayTitle(
    day: AgendaDayRow,
    texts: AgendaTexts,
    accent: Color,
) {
    val flags = listOfNotNull(texts.today.takeIf { day.isToday }, texts.holiday.takeIf { day.isHoliday })
    Text(
        (listOf(day.longDate) + flags).joinToString(texts.separator),
        color = accent,
        style = MaterialTheme.typography.bodyLarge,
    )
    if (day.otherDates.isNotEmpty()) {
        Text(
            day.otherDates.joinToString(texts.separator),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayEvents(
    day: AgendaDayRow,
    texts: AgendaTexts,
    onAction: (AgendaAction) -> Unit,
) {
    if (day.events.isEmpty()) {
        Text(
            texts.noEvents,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.agenda_event_description)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        day.events.forEach { event ->
            val color =
                when (event.kind) {
                    AgendaEventKind.OFFICIAL -> colors.primary
                    AgendaEventKind.PERSONAL -> colors.tertiary
                    AgendaEventKind.DEVICE -> colors.secondary
                    AgendaEventKind.SUBSCRIPTION -> colors.outline
                }
            val spoken = description.format(event.title, texts.kind(event))
            // T-703: a personal event's chip grows into its editor.
            val key = if (event.kind == AgendaEventKind.PERSONAL) SharedKey.Event(event.id) else null
            WithSharedBounds(key) { shared ->
                EventChip(
                    EventChipModel(event.title, color, spoken, event.isHoliday),
                    modifier = shared,
                    onClick = { onAction(AgendaAction.OpenEvent(event)) },
                )
            }
        }
    }
}
