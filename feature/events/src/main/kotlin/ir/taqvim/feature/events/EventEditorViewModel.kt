/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Jdn
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The personal event editor (T-1000): creates the event [eventId] is `null` for, otherwise edits the stored one;
 * validates before saving and reports the outcome through [EditorContent.Finished].
 */
class EventEditorViewModel(
    private val eventId: Long?,
    private val store: PersonalEventStore,
    settingsSource: EditorSettingsSource,
    private val clock: Clock,
) : ViewModel() {
    private val session = MutableStateFlow<EditorSession>(EditorSession.Loading)
    private val latestSettings = MutableStateFlow<EditorSettings?>(null)

    val uiState: StateFlow<EventEditorUiState> =
        combine(settingsSource.settings().onEach(::onSettings), session) { settings, current ->
            EditorPresenter.present(current, settings)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventEditorUiState())

    /** Applies a form change. */
    fun onIntent(intent: EditorIntent) {
        val settings = latestSettings.value ?: return
        editing { it.copy(form = EditorReducer.reduce(it.form, intent, settings), storeFailed = false) }
    }

    fun onDateTextChange(text: String) {
        editing { it.copy(dateText = text, dateTextUnrecognized = false) }
    }

    /** Sets the dates from the typed phrase, or flags it when it contains no date. */
    fun onApplyDateText() {
        val settings = latestSettings.value ?: return
        val today = today(settings)
        editing { current ->
            DatePhrase
                .apply(current.form, current.dateText, settings, today)
                ?.let { current.copy(form = it, dateText = "", dateTextUnrecognized = false) }
                ?: current.copy(dateTextUnrecognized = true)
        }
    }

    /** Saves a valid form; an invalid one shows its errors instead. */
    fun onSave() {
        val settings = latestSettings.value
        val current = session.value as? EditorSession.Editing
        if (settings == null || current == null || current.busy) return
        val calendar = settings.arithmeticOf(current.form.calendar)
        val valid = EventValidator.validate(current.form, calendar).isEmpty()
        session.value = current.copy(showErrors = true, busy = valid, storeFailed = false)
        if (valid) {
            viewModelScope.launch {
                val saved = runCatching { store.save(current.form.toEvent(calendar)) }
                finishOrFail(saved.isSuccess, EditorOutcome.SAVED, current.copy(showErrors = true))
            }
        }
    }

    /** Deletes a stored event; a new one is simply discarded. */
    fun onDelete() {
        val current = session.value as? EditorSession.Editing ?: return
        val id = current.form.id
        if (id == null) {
            session.value = EditorSession.Finished(EditorOutcome.DISCARDED)
        } else if (!current.busy) {
            session.value = current.copy(busy = true, storeFailed = false)
            viewModelScope.launch {
                finishOrFail(runCatching { store.delete(id) }.isSuccess, EditorOutcome.DELETED, current)
            }
        }
    }

    /** Closes the editor without saving. */
    fun onDiscard() {
        if (session.value is EditorSession.Editing) session.value = EditorSession.Finished(EditorOutcome.DISCARDED)
    }

    private suspend fun onSettings(settings: EditorSettings) {
        latestSettings.value = settings
        if (session.value == EditorSession.Loading) session.value = open(settings)
    }

    private suspend fun open(settings: EditorSettings): EditorSession {
        val id = eventId
        val form =
            if (id == null) {
                EditorForm.new(settings, today(settings))
            } else {
                runCatching { store.load(id) }
                    .getOrNull()
                    ?.takeIf { it.calendar in settings.arithmetic }
                    ?.let { EditorForm.of(it, settings.arithmeticOf(it.calendar), settings.language.numerals) }
            }
        return form?.let { EditorSession.Editing(form = it, original = it) } ?: EditorSession.NotFound
    }

    private fun finishOrFail(
        succeeded: Boolean,
        outcome: EditorOutcome,
        before: EditorSession.Editing,
    ) {
        session.value =
            if (succeeded) EditorSession.Finished(outcome) else before.copy(busy = false, storeFailed = true)
    }

    private fun today(settings: EditorSettings): Jdn = clock.now().toJdn(TimeZone.of(settings.timeZoneId))

    private inline fun editing(change: (EditorSession.Editing) -> EditorSession.Editing) {
        session.update { current -> (current as? EditorSession.Editing)?.let(change) ?: current }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
