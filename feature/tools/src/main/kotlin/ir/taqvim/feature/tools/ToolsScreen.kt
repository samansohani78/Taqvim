/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

/** The Tools screen (T-1400), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun ToolsScreen(
    state: ToolsUiState,
    actions: ToolsActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.tools_title)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SegmentedTabs(
                tabs = ToolsTab.entries.map { stringResource(it.label) },
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onSelectTab(ToolsTab.entries[it]) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val content = state.content) {
                ToolsContent.Loading -> Loading()
                is ToolsContent.Ready -> ToolContent(state.tab, state.inputs, content, actions)
            }
        }
    }
}

@Composable
private fun Loading() {
    val description = stringResource(R.string.tools_loading)
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center).semantics { contentDescription = description })
    }
}

@Composable
private fun ToolContent(
    tab: ToolsTab,
    inputs: ToolsInputs,
    content: ToolsContent.Ready,
    actions: ToolsActions,
) {
    val change = actions.onInputsChange
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (tab) {
            ToolsTab.CONVERTER -> {
                ConverterTool(inputs.converter, content.converter) { change(inputs.copy(converter = it)) }
            }

            ToolsTab.DISTANCE -> {
                DistanceTool(inputs, content.distance, change)
            }

            ToolsTab.DURATION -> {
                DurationTool(inputs.duration, content.duration) { change(inputs.copy(duration = it)) }
            }

            ToolsTab.TIME_ZONES -> {
                TimeZonesTool(inputs, content.board, actions)
            }

            ToolsTab.QR -> {
                QrTool(inputs.qr, content.qr, actions) { change(inputs.copy(qr = it)) }
            }
        }
    }
}

/** A single-line text field with a label and an optional supporting line. */
@Composable
internal fun ToolField(
    value: String,
    @StringRes label: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A label and its value on one line, read together by accessibility services. */
@Composable
internal fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** A message line; [isError] uses the error color. */
@Composable
internal fun ToolMessage(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@get:StringRes
internal val ToolsTab.label: Int
    get() =
        when (this) {
            ToolsTab.CONVERTER -> R.string.tools_tab_converter
            ToolsTab.DISTANCE -> R.string.tools_tab_distance
            ToolsTab.DURATION -> R.string.tools_tab_duration
            ToolsTab.TIME_ZONES -> R.string.tools_tab_time_zones
            ToolsTab.QR -> R.string.tools_tab_qr
        }

@get:StringRes
internal val CalendarSystem.label: Int
    get() =
        when (this) {
            CalendarSystem.PERSIAN -> R.string.tools_calendar_persian
            CalendarSystem.ISLAMIC -> R.string.tools_calendar_islamic
            CalendarSystem.GREGORIAN -> R.string.tools_calendar_gregorian
            CalendarSystem.NEPALI -> R.string.tools_calendar_nepali
        }
