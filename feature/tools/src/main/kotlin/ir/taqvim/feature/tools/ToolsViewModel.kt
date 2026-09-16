/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.attempt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * The Tools screen (T-1400): date converter and day distance with NLP input, duration calculator, time-zone board and
 * QR codes. Only the selected tool is computed, on [computeDispatcher], and only when its own input, the settings or
 * (for dates) the day changes; a newer input replaces work that has not started (code review I05). The board follows
 * the clock minute by minute. Tools that were not opened yet keep an empty placeholder result, which is never shown
 * because the state switches tabs together with the new tool's result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModel(
    settingsSource: ToolsSettingsSource,
    clock: Clock,
    /** Where time-zone board edits are kept between sessions. */
    private val boardStore: ToolsBoardStore = ToolsBoardStore.NONE,
    /** Text the converter opens with (a T-1103 link or selected text); `null` opens it empty. */
    initialConverterText: String? = null,
    /** Where the tools compute, so parsing a long pasted text or encoding a QR code stays off the main thread. */
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val tab = MutableStateFlow(ToolsTab.CONVERTER)
    private val inputs = MutableStateFlow(ToolsInputs(converter = initialConverterText.orEmpty()))

    /** Board zones changed in this session; `null` while the settings' zones are shown unchanged. */
    private val chosenZones = MutableStateFlow<List<String>?>(null)
    private val latestSettings = MutableStateFlow<ToolsSettings?>(null)
    private val settings =
        settingsSource
            .settings()
            .onEach { latestSettings.value = it }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)
    private val ticks = minuteTicks(clock).shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
    private val today = combine(settings, ticks) { current, now -> now.toJdn(current.homeZone) }.distinctUntilChanged()

    /** The latest result of every tool; the selected tool's result replaces its entry. */
    private val results = MutableStateFlow(EMPTY_RESULTS)

    private val shown: Flow<Shown> =
        tab.flatMapLatest { selected ->
            changes(selected)
                .flowOn(computeDispatcher)
                .map { change -> Shown(selected, results.updateAndGet(change)) }
        }

    val uiState: StateFlow<ToolsUiState> =
        combine(inputs, shown.onStart { emit(Shown(tab.value, ToolsContent.Loading)) }) { typed, current ->
            ToolsUiState(current.tab, typed, current.content)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ToolsUiState())

    /** The selected tool's results as changes to [results]; everything upstream runs on [computeDispatcher]. */
    private fun changes(selected: ToolsTab): Flow<(ToolsContent.Ready) -> ToolsContent.Ready> =
        when (selected) {
            ToolsTab.CONVERTER -> {
                combine(settings, input { it.converter }, today, ::Triple)
                    .mapLatest { (current, text, day) -> DateTools.convert(text, day, current) }
                    .map { result -> { ready: ToolsContent.Ready -> ready.copy(converter = result) } }
            }

            ToolsTab.DISTANCE -> {
                combine(settings, input { it.distanceFrom to it.distanceTo }, today, ::Triple)
                    .mapLatest { (current, ends, day) -> DateTools.distance(ends.first, ends.second, day, current) }
                    .map { result -> { ready: ToolsContent.Ready -> ready.copy(distance = result) } }
            }

            ToolsTab.DURATION -> {
                combine(settings, input { it.duration }, ::Pair)
                    .mapLatest { (current, text) -> DurationPresenter.present(text, current.language) }
                    .map { result -> { ready: ToolsContent.Ready -> ready.copy(duration = result) } }
            }

            ToolsTab.TIME_ZONES -> {
                combine(settings, chosenZones, input { it.zoneQuery }, ticks) { current, zones, query, now ->
                    TimeZoneBoardBuilder.build(now, current, zones ?: current.boardZones, query)
                }.map { result -> { ready: ToolsContent.Ready -> ready.copy(board = result) } }
            }

            ToolsTab.QR -> {
                input { it.qr }
                    .mapLatest(QrEncoder::encode)
                    .map { result -> { ready: ToolsContent.Ready -> ready.copy(qr = result) } }
            }
        }

    fun onSelectTab(selected: ToolsTab) {
        tab.value = selected
    }

    fun onInputsChange(typed: ToolsInputs) {
        inputs.value = typed
    }

    /** Adds [id] to the board and clears the search. */
    fun onAddZone(id: String) {
        editBoard { zones -> zones.filterNot { it == id } + id }
        inputs.update { it.copy(zoneQuery = "") }
    }

    fun onRemoveZone(id: String) {
        editBoard { zones -> zones.filterNot { it == id } }
    }

    /** Shows the board changed by [change] and keeps it; when keeping fails the change still lasts for the session. */
    private fun editBoard(change: (List<String>) -> List<String>) {
        val zones = chosenZones.updateAndGet { current -> change(current ?: settingsZones()) }.orEmpty()
        viewModelScope.launch { attempt { boardStore.setBoardZones(zones) } }
    }

    /** The QR text and its code while one is shown, for sharing. */
    fun qrToShare(): Pair<String, QrMatrix>? {
        val state = uiState.value
        val code = (state.content as? ToolsContent.Ready)?.qr as? QrState.Code
        return code?.let { state.inputs.qr to it.matrix }
    }

    private fun settingsZones(): List<String> = latestSettings.value?.boardZones.orEmpty()

    private fun <T> input(select: (ToolsInputs) -> T): Flow<T> = inputs.map(select).distinctUntilChanged()

    /** The selected tab and what it shows; the tab changes together with its first result. */
    private data class Shown(
        val tab: ToolsTab,
        val content: ToolsContent,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Placeholders for tools not computed yet. */
        val EMPTY_RESULTS =
            ToolsContent.Ready(
                converter = ConverterResult.NotRecognized,
                distance = DistanceState(),
                duration = DurationState.Empty,
                board = TimeZoneBoard(persistentListOf(), persistentListOf()),
                qr = QrState.Empty,
            )
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

/** [clock]'s time now and then at every following minute boundary. */
internal fun minuteTicks(clock: Clock): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
        }
    }
