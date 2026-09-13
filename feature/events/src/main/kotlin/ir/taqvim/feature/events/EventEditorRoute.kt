/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the event editor; `:app` provides [PersonalEventStore], [EditorSettingsSource] and
 * `kotlin.time.Clock`. The event id (or `null` for a new event) is the view model's parameter.
 */
val eventsFeatureModule: Module =
    module {
        viewModel { parameters -> EventEditorViewModel(parameters.getOrNull(), get(), get(), get()) }
    }

/** The editor of event [eventId] (`null` creates one); [onClose] is called once with how it was closed. */
@Composable
fun EventEditorRoute(
    eventId: Long?,
    onClose: (EditorOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventEditorViewModel =
        koinViewModel(key = "event-editor-$eventId", parameters = { parametersOf(eventId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    val finished = state.content as? EditorContent.Finished
    LaunchedEffect(finished) { finished?.let { currentOnClose(it.outcome) } }
    val actions =
        remember(viewModel) {
            EventEditorActions(
                onIntent = viewModel::onIntent,
                onDateTextChange = viewModel::onDateTextChange,
                onApplyDateText = viewModel::onApplyDateText,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onDiscard = viewModel::onDiscard,
            )
        }
    EventEditorScreen(state, actions, modifier)
}
