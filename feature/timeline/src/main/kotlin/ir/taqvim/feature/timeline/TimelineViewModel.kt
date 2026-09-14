/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The timeline (T-900): the shown day or week, the zoom, and the box of a new event with the events and prayer lines of
 * the shown days. Actions arrive through [onAction]; creating and opening events leave through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    settingsSource: TimelineSettingsSource,
    clockSource: TimelineClockSource,
    placeSource: TimelinePlaceSource,
    private val daysSource: TimelineDaysSource,
) : ViewModel() {
    private val view = MutableStateFlow(TimelineView())
    private val effectChannel = Channel<TimelineEffect>(Channel.BUFFERED)

    private val now: StateFlow<TimelineNow?> = clockSource.now().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val settings: StateFlow<TimelineSettings?> =
        settingsSource.settings().distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val shown: Flow<ShownDays> =
        combine(now.filterNotNull().map { it.day }.distinctUntilChanged(), settings.filterNotNull(), view) {
            today,
            settings,
            view,
            ->
            ShownDays(settings, view, TimelineContentBuilder.range(view.mode, view.anchor ?: today, settings.weekStart))
        }.distinctUntilChanged()

    private val ranges: Flow<JdnRange> = shown.map { it.range }.distinctUntilChanged()

    /** Events of the shown days; a new range emits `null` until its days load. */
    private val days: Flow<LoadedDays?> =
        ranges
            .flatMapLatest { range ->
                daysSource
                    .days(range)
                    .map<List<TimelineDay>, LoadedDays?> { LoadedDays(range, it) }
                    .onStart { emit(null) }
            }.onStart { emit(null) }

    /** Prayer lines of the shown days, computed again only when the days or the place change. */
    private val lines: Flow<Map<Jdn, ImmutableList<PrayerLine>>> =
        combine(ranges, placeSource.place().onStart { emit(null) }.distinctUntilChanged()) { range, place ->
            range.associateWith { TimelineContentBuilder.prayerLines(it, place) }
        }

    /** One-shot effects: create or open an event. */
    val effects: Flow<TimelineEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<TimelineUiState> =
        combine(shown, days, lines, now.filterNotNull()) { shown, days, lines, now ->
            TimelineUiState(shown.content(now, days, lines))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TimelineUiState())

    fun onAction(action: TimelineAction) {
        when (action) {
            is TimelineAction.Navigation -> navigate(action)
            is TimelineAction.Draft -> draw(action)
            is TimelineAction.ZoomBy -> zoom(action.factor)
            is TimelineAction.OpenEvent -> effectChannel.trySend(TimelineEffect.NavigateToEvent(action.id, action.kind))
        }
    }

    private fun zoom(factor: Float) {
        view.update { it.copy(zoom = TimelineGeometry.clampZoom(it.zoom * factor)) }
    }

    private fun navigate(action: TimelineAction.Navigation) {
        when (action) {
            is TimelineAction.ShowMode -> {
                view.update { it.copy(mode = action.mode, draft = null) }
            }

            TimelineAction.ShowNext -> {
                shift(1)
            }

            TimelineAction.ShowPrevious -> {
                shift(-1)
            }

            TimelineAction.GoToToday -> {
                view.update { it.copy(anchor = null, draft = null) }
            }

            is TimelineAction.ShowWeekOf -> {
                view.update { it.copy(mode = TimelineMode.WEEK, anchor = action.day, draft = null) }
            }
        }
    }

    /** Moves the anchor by one day or week; ignored before today is known. */
    private fun shift(direction: Int) {
        val today = now.value?.day ?: return
        view.update { state ->
            val step = if (state.mode == TimelineMode.DAY) 1 else DAYS_PER_WEEK
            state.copy(anchor = (state.anchor ?: today) + direction * step, draft = null)
        }
    }

    private fun draw(action: TimelineAction.Draft) {
        when (action) {
            is TimelineAction.SetDraft -> {
                view.update { it.copy(draft = TimelineDraft.spanning(action.day, action.fromMinute, action.toMinute)) }
            }

            is TimelineAction.MoveDraft -> {
                view.update { it.copy(draft = it.draft?.moved(action.steps)) }
            }

            is TimelineAction.ResizeDraft -> {
                view.update { it.copy(draft = it.draft?.resized(action.steps)) }
            }

            TimelineAction.CancelDraft -> {
                view.update { it.copy(draft = null) }
            }

            TimelineAction.ConfirmDraft -> {
                confirm()
            }
        }
    }

    private fun confirm() {
        val draft = view.value.draft ?: return
        view.update { it.copy(draft = null) }
        effectChannel.trySend(TimelineEffect.CreateEvent(draft.day, draft.startMinute, draft.endMinute))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DAYS_PER_WEEK = 7
    }
}

/** What the user chose; a `null` anchor follows today. */
private data class TimelineView(
    val mode: TimelineMode = TimelineMode.WEEK,
    val anchor: Jdn? = null,
    val zoom: Float = TimelineGeometry.DEFAULT_ZOOM,
    val draft: TimelineDraft? = null,
)

private data class LoadedDays(
    val range: JdnRange,
    val days: List<TimelineDay>,
)

/** The view resolved against today and the preferences. */
private data class ShownDays(
    val settings: TimelineSettings,
    val view: TimelineView,
    val range: JdnRange,
) {
    fun content(
        now: TimelineNow,
        days: LoadedDays?,
        lines: Map<Jdn, ImmutableList<PrayerLine>>,
    ): TimelineContent {
        val loaded = days?.takeIf { it.range == range }
        val calendar = TimelineContentBuilder.primaryCalendar(settings.calendars, settings.islamicVariant)
        return TimelineContent(
            now = now,
            mode = view.mode,
            columns =
                TimelineContentBuilder.columns(range, loaded?.days.orEmpty()) { day ->
                    lines[day] ?: emptyList<PrayerLine>().toImmutableList()
                },
            zoom = view.zoom,
            draft = view.draft,
            calendar = calendar.system,
            islamicVariant = settings.islamicVariant,
            languageCode = settings.languageCode,
            isLoaded = loaded != null,
        )
    }
}
