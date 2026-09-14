/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.LanguageSpec

/** "Remind me" choices under an official event's source card: one chip per lead time, on or off. */
@Composable
internal fun OfficialReminderRow(
    choices: OfficialReminderChoices,
    language: LanguageSpec,
    onAction: (CalendarAction) -> Unit,
) {
    val resources = LocalResources.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.calendar_remind_me),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OfficialReminderChoices.PRESETS.forEach { days ->
                val selected = days in choices.enabled
                FilterChip(
                    selected = selected,
                    onClick = { onAction(CalendarAction.ToggleOfficialReminder(days, !selected)) },
                    label = { Text(reminderLabel(resources, days, language)) },
                )
            }
        }
    }
}

/** The label of a reminder [days] before an official event: "On the day" or "N days before". */
internal fun reminderLabel(
    resources: Resources,
    days: Int,
    language: LanguageSpec,
): String =
    if (days == 0) {
        resources.getString(R.string.calendar_remind_same_day)
    } else {
        resources.getQuantityString(R.plurals.calendar_remind_days_before, days, number(days.toLong(), language))
    }
