/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The month list and agenda (T-901): a list anchored at today that loads months as either end is reached. Rows are
 * built on [dispatcher]; navigation leaves through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(
    settingsSource: AgendaSettingsSource,
    todaySource: AgendaTodaySource,
    private val daySource: AgendaDaySource,
    dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val window = MutableStateFlow(AgendaWindow.INITIAL)
    private val loadedWindow = MutableStateFlow<AgendaWindow?>(null)
    private val mode = MutableStateFlow(AgendaMode.AGENDA)
    private val scrollRequests = MutableStateFlow(0)
    private val effectChannel = Channel<AgendaEffect>(Channel.BUFFERED)

    private val loaded: Flow<Loaded> =
        combine(
            todaySource.today().distinctUntilChanged(),
            settingsSource.settings().distinctUntilChanged(),
            window,
        ) { today, settings, window -> Request(today, AgendaCalendars(settings), window) }
            .flatMapLatest { request -> daySource.days(request.range()).map { Loaded(request, it) } }
            .onEach { loadedWindow.value = it.request.window }

    private val lists: Flow<Built> =
        combine(loaded, mode) { loaded, mode ->
            val builder = AgendaListBuilder(loaded.request.calendars, loaded.request.language())
            Built(loaded, mode, builder.build(loaded.request.today, loaded.request.window, loaded.days, mode))
        }.flowOn(dispatcher)

    /** One-shot navigation effects. */
    val effects: Flow<AgendaEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<AgendaUiState> =
        combine(lists, window, scrollRequests) { built, window, scroll ->
            AgendaUiState(content(built, window, scroll))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AgendaUiState())

    fun onAction(action: AgendaAction) {
        when (action) {
            AgendaAction.LoadEarlier -> load { it.canLoadEarlier to it.earlier() }
            AgendaAction.LoadLater -> load { it.canLoadLater to it.later() }
            AgendaAction.GoToToday -> goToToday()
            is AgendaAction.SelectMode -> selectMode(action.mode)
            is AgendaAction.OpenDay -> emit(AgendaEffect.NavigateToDay(action.jdn))
            is AgendaAction.OpenEvent -> emit(AgendaEffect.NavigateToEvent(action.event))
        }
    }

    /** Moves the window by [next] once the current window has loaded; ignored while loading or at the limit. */
    private fun load(next: (AgendaWindow) -> Pair<Boolean, AgendaWindow>) {
        window.update { current ->
            val (possible, moved) = next(current)
            if (possible && loadedWindow.value == current) moved else current
        }
    }

    private fun goToToday() {
        window.update { if (0 in it) it else AgendaWindow.INITIAL }
        scrollRequests.update { it + 1 }
    }

    private fun selectMode(selected: AgendaMode) {
        if (mode.value != selected) {
            mode.value = selected
            scrollRequests.update { it + 1 }
        }
    }

    private fun emit(effect: AgendaEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun content(
        built: Built,
        window: AgendaWindow,
        scroll: Int,
    ): AgendaContent {
        val loaded = built.loaded
        val language = loaded.request.language()
        return AgendaContent(
            today = loaded.request.today,
            mode = built.mode,
            items = built.list.items,
            todayIndex = built.list.todayIndex,
            canLoadEarlier = window.canLoadEarlier,
            canLoadLater = window.canLoadLater,
            isLoading = loaded.request.window != window,
            scrollToTodayRequest = scroll,
            localeTag = language.localeTag,
            isRightToLeft = language.direction == TextDirection.RTL,
        )
    }

    private data class Request(
        val today: Jdn,
        val calendars: AgendaCalendars,
        val window: AgendaWindow,
    ) {
        fun range() = calendars.range(today, window.first, window.last)

        fun language() = AgendaListBuilder.languageFor(calendars.settings.languageCode)
    }

    private class Loaded(
        val request: Request,
        val days: List<AgendaDay>,
    )

    private class Built(
        val loaded: Loaded,
        val mode: AgendaMode,
        val list: AgendaList,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
