/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.taqvim.core.events.Citation
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.ui.component.ComponentAction
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.EventChip
import ir.taqvim.core.ui.component.EventChipModel
import ir.taqvim.core.ui.component.TooltipCard
import ir.taqvim.core.ui.motion.SharedKey
import ir.taqvim.core.ui.motion.WithSharedBounds
import kotlinx.collections.immutable.ImmutableList

/**
 * The Events tab: one chip per event of the selected day. An official event shows its source and citation and the
 * reminders before it (T-1002); other
 * events (personal, device, subscribed) open. A day without events offers to add one.
 */
@Composable
internal fun DayEventsTab(
    content: CalendarContent,
    language: LanguageSpec,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = content.dayDetails
    when {
        details == null -> {
            DetailsLoading(stringResource(R.string.calendar_details_loading), modifier)
        }

        details.events.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.calendar_no_events),
                modifier = modifier,
                action =
                    ComponentAction(stringResource(R.string.calendar_add_event)) {
                        onAction(CalendarAction.CreateEvent(details.jdn))
                    },
            )
        }

        else -> {
            EventList(details.events, content.sourceEvent, content.officialReminders, language, onAction, modifier)
        }
    }
}

/** The shared-bounds key of [event]'s chip: personal events grow into the editor they open (T-703). */
private fun chipKey(event: DayEventItem): SharedKey? =
    if (event.kind == DayEventKind.PERSONAL) SharedKey.Event(event.id) else null

@Composable
private fun EventList(
    events: ImmutableList<DayEventItem>,
    sourceEvent: DayEventItem?,
    reminders: OfficialReminderChoices?,
    language: LanguageSpec,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier,
) {
    val resources = LocalResources.current
    val palette = MaterialTheme.colorScheme.indicatorPalette()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { event ->
            WithSharedBounds(chipKey(event)) { shared ->
                EventChip(
                    model =
                        EventChipModel(
                            event.title,
                            palette.colorOf(event),
                            chipDescription(resources, event),
                            event.isHoliday,
                        ),
                    modifier = shared,
                    onClick = {
                        val action =
                            if (event.kind == DayEventKind.OFFICIAL) {
                                CalendarAction.ShowEventSource(event)
                            } else {
                                CalendarAction.OpenEvent(event)
                            }
                        onAction(action)
                    },
                )
            }
            if (event == sourceEvent) {
                EventSourceCard(event, language, onAction)
                reminders?.takeIf { it.eventId == event.id }?.let { OfficialReminderRow(it, language, onAction) }
            }
        }
    }
}

@Composable
private fun EventSourceCard(
    event: DayEventItem,
    language: LanguageSpec,
    onAction: (CalendarAction) -> Unit,
) {
    val resources = LocalResources.current
    val citation = event.citations.firstOrNull()
    TooltipCard(
        title = sourceLabel(resources, event),
        body = event.title,
        modifier = Modifier.fillMaxWidth(),
        footnote =
            citation?.let { citationText(resources, it, language) } ?: stringResource(R.string.calendar_no_citation),
        action =
            citation?.let {
                ComponentAction(stringResource(R.string.calendar_open_source)) {
                    onAction(CalendarAction.OpenCitation(it.url))
                }
            },
        dismiss =
            ComponentAction(
                stringResource(R.string.calendar_close),
            ) { onAction(CalendarAction.DismissEventSource) },
    )
}

/** What accessibility services read for an event chip: its title, where it comes from and whether it is a holiday. */
internal fun chipDescription(
    resources: Resources,
    event: DayEventItem,
): String =
    listOfNotNull(
        event.title,
        sourceLabel(resources, event),
        resources.getString(R.string.calendar_holiday).takeIf { event.isHoliday },
    ).joinToString(resources.getString(R.string.calendar_separator))

/** The dataset source of an official event, or the kind of any other event. */
internal fun sourceLabel(
    resources: Resources,
    event: DayEventItem,
): String {
    val source = event.source
    return if (event.kind == DayEventKind.OFFICIAL && source != null) {
        resources.getString(DayDetailsLabels.of(source))
    } else {
        resources.getString(DayDetailsLabels.of(event.kind))
    }
}

private fun citationText(
    resources: Resources,
    citation: Citation,
    language: LanguageSpec,
): String {
    val page = citation.page ?: return citation.title
    val localizedPage = page.toLongOrNull()?.let { number(it, language) } ?: page
    return resources.getString(R.string.calendar_citation_page, citation.title, localizedPage)
}
