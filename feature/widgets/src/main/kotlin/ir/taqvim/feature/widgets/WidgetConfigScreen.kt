/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import kotlin.math.roundToInt

/** Stateless configuration screen of one widget (T-1200). */
@Composable
fun WidgetConfigScreen(
    state: WidgetConfigUiState,
    actions: WidgetConfigActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(title = stringResource(R.string.widget_config_title)) },
        bottomBar = { ConfigButtons(state, actions) },
    ) { padding ->
        if (state.loading) {
            val loading = stringResource(R.string.widget_config_loading)
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BackgroundSection(state.config, actions)
                TransparencySection(state.config, actions)
                ScaleSection(state.config, actions)
                ContentsSection(state, actions)
                SecondaryCalendarSection(state, actions)
                val choices = state.countdown
                val countdown = state.config.countdown
                if (choices != null && countdown != null) CountdownSection(choices, countdown, actions.countdown)
            }
        }
    }
}

@Composable
private fun ConfigButtons(
    state: WidgetConfigUiState,
    actions: WidgetConfigActions,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        if (state.saveFailed) {
            Text(
                stringResource(R.string.widget_config_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = actions.onCancel) { Text(stringResource(R.string.widget_config_cancel)) }
            Button(onClick = actions.onSave, enabled = !state.loading && !state.saving) {
                Text(stringResource(R.string.widget_config_save))
            }
        }
    }
}

@Composable
internal fun SectionTitle(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun BackgroundSection(
    config: WidgetConfig,
    actions: WidgetConfigActions,
) {
    SectionTitle(R.string.widget_config_background)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetBackground.entries.forEach { background ->
            FilterChip(
                selected = config.background == background,
                onClick = { actions.onBackground(background) },
                label = { Text(stringResource(background.label)) },
            )
        }
    }
}

@Composable
private fun TransparencySection(
    config: WidgetConfig,
    actions: WidgetConfigActions,
) {
    val value = stringResource(R.string.widget_config_percent, config.transparencyPercent)
    val label = stringResource(R.string.widget_config_transparency)
    SectionTitle(R.string.widget_config_transparency)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(
            value = config.transparencyPercent.toFloat(),
            onValueChange = { actions.onTransparency(it.roundToInt()) },
            valueRange = 0f..WidgetConfig.MAX_TRANSPARENCY.toFloat(),
            steps = WidgetConfig.MAX_TRANSPARENCY / WidgetConfig.TRANSPARENCY_STEP - 1,
            modifier =
                Modifier.weight(1f).semantics {
                    contentDescription = label
                    stateDescription = value
                },
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScaleSection(
    config: WidgetConfig,
    actions: WidgetConfigActions,
) {
    SectionTitle(R.string.widget_config_scale)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetConfig.SCALES.forEach { scale ->
            FilterChip(
                selected = config.scalePercent == scale,
                onClick = { actions.onScale(scale) },
                label = { Text(stringResource(R.string.widget_config_percent, scale)) },
            )
        }
    }
}

@Composable
private fun ContentsSection(
    state: WidgetConfigUiState,
    actions: WidgetConfigActions,
) {
    val offered = WidgetContent.entries.filter { it in state.kind.contents }
    if (offered.isEmpty()) return
    SectionTitle(R.string.widget_config_contents)
    offered.forEach { content ->
        val shown = state.config.shows(content)
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = shown, role = Role.Switch, onValueChange = { actions.onContent(content, it) })
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(content.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = shown, onCheckedChange = null)
        }
    }
}

@Composable
private fun SecondaryCalendarSection(
    state: WidgetConfigUiState,
    actions: WidgetConfigActions,
) {
    if (WidgetContent.SECONDARY_DATE !in state.kind.contents) return
    SectionTitle(R.string.widget_config_secondary_calendar)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.config.secondaryCalendar == null,
            onClick = { actions.onSecondaryCalendar(null) },
            label = { Text(stringResource(R.string.widget_calendar_automatic)) },
        )
        state.calendars.forEach { calendar ->
            FilterChip(
                selected = state.config.secondaryCalendar == calendar,
                onClick = { actions.onSecondaryCalendar(calendar) },
                label = { Text(stringResource(calendarLabel(calendar))) },
            )
        }
    }
}

/** String resource of a background choice. */
@get:StringRes
internal val WidgetBackground.label: Int
    get() =
        when (this) {
            WidgetBackground.SURFACE -> R.string.widget_background_surface
            WidgetBackground.PRIMARY_CONTAINER -> R.string.widget_background_primary
            WidgetBackground.SECONDARY_CONTAINER -> R.string.widget_background_secondary
            WidgetBackground.BLACK -> R.string.widget_background_black
            WidgetBackground.WHITE -> R.string.widget_background_white
        }

/** String resource of an optional widget part. */
@get:StringRes
internal val WidgetContent.label: Int
    get() =
        when (this) {
            WidgetContent.WEEKDAY -> R.string.widget_content_weekday
            WidgetContent.SECONDARY_DATE -> R.string.widget_content_secondary_date
            WidgetContent.HOLIDAYS -> R.string.widget_content_holidays
            WidgetContent.EVENTS -> R.string.widget_content_events
            WidgetContent.NEXT_PRAYER -> R.string.widget_content_next_prayer
        }

/** String resource of a calendar's name. */
@StringRes
internal fun calendarLabel(calendar: CalendarSystem): Int =
    when (calendar) {
        CalendarSystem.PERSIAN -> R.string.widget_calendar_persian
        CalendarSystem.ISLAMIC -> R.string.widget_calendar_islamic
        CalendarSystem.GREGORIAN -> R.string.widget_calendar_gregorian
        CalendarSystem.NEPALI -> R.string.widget_calendar_nepali
    }
