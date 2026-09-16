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
import ir.taqvim.core.ui.permission.rememberNotificationPermissionRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the event editor; `:app` provides [PersonalEventStore], [EditorSettingsSource] and
 * `kotlin.time.Clock`. The event id (or `null` for a new event) and an optional [NewEventDraft] are the view model's
 * parameters; its `SavedStateHandle` comes from the navigation entry.
 */
val eventsFeatureModule: Module =
    module {
        viewModel { parameters ->
            val draft = parameters.getOrNull<NewEventDraft>()
            val occurrence = parameters.getOrNull<OccurrenceTarget>()
            // B11: Koin passes the entry's SavedStateHandle, so an unsaved draft survives process death.
            EventEditorViewModel(parameters.getOrNull<Long>(), get(), get(), get(), get(), draft, occurrence)
        }
    }

/**
 * The editor of event [eventId] (`null` creates one, starting from [draft] when given); a repeating event opened from
 * one of its days passes that [occurrence] so the user can change only it (ADR-0034). [onClose] is called once with
 * how it was closed.
 */
@Composable
fun EventEditorRoute(
    eventId: Long?,
    onClose: (EditorOutcome) -> Unit,
    modifier: Modifier = Modifier,
    draft: NewEventDraft? = null,
    occurrence: OccurrenceTarget? = null,
    viewModel: EventEditorViewModel =
        koinViewModel(
            key = "event-editor-$eventId-$draft-$occurrence",
            parameters = { parametersOf(eventId, draft, occurrence) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    val finished = state.content as? EditorContent.Finished
    LaunchedEffect(finished) { finished?.let { currentOnClose(it.outcome) } }
    val requestNotifications = rememberNotificationPermissionRequest()
    val actions =
        remember(viewModel, requestNotifications) {
            EventEditorActions(
                onIntent = { intent ->
                    viewModel.onIntent(intent)
                    // A reminder needs notifications to show (T-1001).
                    if (intent is ReminderIntent.Add) requestNotifications()
                },
                onDateTextChange = viewModel::onDateTextChange,
                onApplyDateText = viewModel::onApplyDateText,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onDiscard = viewModel::onDiscard,
                onChooseScope = viewModel::onChooseScope,
            )
        }
    EventEditorScreen(state, actions, modifier)
}
