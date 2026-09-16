/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.attempt
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The personal event editor (T-1000): creates the event [eventId] is `null` for, otherwise edits the stored one;
 * validates before saving and reports the outcome through [EditorContent.Finished]. A new event starts from [draft]
 * when one is given (a day of the calendar or a range drawn on the timeline), otherwise all-day today. Unsaved changes
 * are kept in [savedState] so they survive process death (B11) and are cleared once the editor finishes.
 */
class EventEditorViewModel(
    private val eventId: Long?,
    private val store: PersonalEventStore,
    settingsSource: EditorSettingsSource,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
    private val draft: NewEventDraft? = null,
) : ViewModel() {
    private val session = MutableStateFlow<EditorSession>(EditorSession.Loading)

    init {
        session.onEach(::keepDraft).launchIn(viewModelScope)
    }

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
            viewModelScope
                .launch {
                    val saved = attempt { store.save(current.form.toEvent(calendar)) }
                    finishOrFail(saved.isSuccess, EditorOutcome.SAVED, current.copy(showErrors = true))
                }.invokeOnCompletion { stopBusy() }
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
            viewModelScope
                .launch {
                    finishOrFail(attempt { store.delete(id) }.isSuccess, EditorOutcome.DELETED, current)
                }.invokeOnCompletion { stopBusy() }
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
                newForm(settings)
            } else {
                attempt { store.load(id) }
                    .getOrNull()
                    ?.takeIf { it.calendar in settings.arithmetic }
                    ?.let { EditorForm.of(it, settings.arithmeticOf(it.calendar), settings.language.numerals) }
            }
        return form?.let(::restoreDraft) ?: EditorSession.NotFound
    }

    /** The form as opened, with the unsaved draft of the same event on top when the process was recreated. */
    private fun restoreDraft(opened: EditorForm): EditorSession.Editing {
        val restored = savedState.get<String>(DRAFT_KEY)?.let { EditorDraftCodec.decode(it, eventId, opened) }
        return EditorSession.Editing(
            form = restored?.form ?: opened,
            original = opened,
            dateText = restored?.dateText.orEmpty(),
        )
    }

    /** Saves the unsaved changes of an editing session and forgets them once the editor finishes. */
    private fun keepDraft(current: EditorSession) {
        when (current) {
            is EditorSession.Editing -> {
                if (current.form == current.original && current.dateText.isEmpty()) {
                    savedState.remove<String>(DRAFT_KEY)
                } else {
                    savedState[DRAFT_KEY] = EditorDraftCodec.encode(eventId, current.form, current.dateText)
                }
            }

            is EditorSession.Finished -> {
                savedState.remove<String>(DRAFT_KEY)
            }

            EditorSession.Loading, EditorSession.NotFound -> {
                // Nothing to keep before the form opens or when there is no event.
            }
        }
    }

    /** Ends a cancelled save or delete: nothing is in progress any more and no failure is shown (review I04). */
    private fun stopBusy() {
        session.update { current ->
            if (current is EditorSession.Editing && current.busy) current.copy(busy = false) else current
        }
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

    /** A new event on the draft's day (or today), timed when the draft has a start minute. */
    private fun newForm(settings: EditorSettings): EditorForm {
        val form = EditorForm.new(settings, draft?.day ?: today(settings))
        val start = draft?.startMinute ?: return form
        val end =
            draft.endMinute?.takeIf { it > start }
                ?: (start + DEFAULT_LENGTH_MINUTES).coerceAtMost(EditorForm.LAST_MINUTE)
        return form.copy(allDay = false, startMinute = start, endMinute = end)
    }

    private inline fun editing(change: (EditorSession.Editing) -> EditorSession.Editing) {
        session.update { current -> (current as? EditorSession.Editing)?.let(change) ?: current }
    }

    internal companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** The `SavedStateHandle` key of the unsaved draft (B11). */
        const val DRAFT_KEY = "event-editor-draft"

        /** Length of a new timed event whose end is not given. */
        private const val DEFAULT_LENGTH_MINUTES = 60
    }
}
