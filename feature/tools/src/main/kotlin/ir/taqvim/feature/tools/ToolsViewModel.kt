/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The Tools screen (T-1400): date converter and day distance with NLP input, duration calculator, time-zone board and
 * QR codes. Each tool is recomputed only when its own input, the settings or (for dates) the day changes; the board
 * follows the clock minute by minute.
 */
class ToolsViewModel(
    settingsSource: ToolsSettingsSource,
    clock: Clock,
) : ViewModel() {
    private val tab = MutableStateFlow(ToolsTab.CONVERTER)
    private val inputs = MutableStateFlow(ToolsInputs())

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

    private val converter =
        combine(settings, input { it.converter }, today) { current, text, day -> DateTools.convert(text, day, current) }
    private val distance =
        combine(settings, input { it.distanceFrom to it.distanceTo }, today) { current, (from, to), day ->
            DateTools.distance(from, to, day, current)
        }
    private val duration =
        combine(settings, input { it.duration }) { current, text -> DurationPresenter.present(text, current.language) }
    private val board =
        combine(settings, chosenZones, input { it.zoneQuery }, ticks) { current, zones, query, now ->
            TimeZoneBoardBuilder.build(now, current, zones ?: current.boardZones, query)
        }
    private val qr = input { it.qr }.map(QrEncoder::encode)

    private val content: Flow<ToolsContent> =
        combine(converter, distance, duration, board, qr) { converted, measured, calculated, zones, code ->
            ToolsContent.Ready(converted, measured, calculated, zones, code)
        }

    val uiState: StateFlow<ToolsUiState> =
        combine(tab, inputs, content.onStart { emit(ToolsContent.Loading) }) { selected, typed, shown ->
            ToolsUiState(selected, typed, shown)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ToolsUiState())

    fun onSelectTab(selected: ToolsTab) {
        tab.value = selected
    }

    fun onInputsChange(typed: ToolsInputs) {
        inputs.value = typed
    }

    /** Adds [id] to the board and clears the search. */
    fun onAddZone(id: String) {
        chosenZones.update { current -> (current ?: settingsZones()).filterNot { it == id } + id }
        inputs.update { it.copy(zoneQuery = "") }
    }

    fun onRemoveZone(id: String) {
        chosenZones.update { current -> (current ?: settingsZones()).filterNot { it == id } }
    }

    /** The QR text and its code while one is shown, for sharing. */
    fun qrToShare(): Pair<String, QrMatrix>? {
        val state = uiState.value
        val code = (state.content as? ToolsContent.Ready)?.qr as? QrState.Code
        return code?.let { state.inputs.qr to it.matrix }
    }

    private fun settingsZones(): List<String> = latestSettings.value?.boardZones.orEmpty()

    private fun <T> input(select: (ToolsInputs) -> T): Flow<T> = inputs.map(select).distinctUntilChanged()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
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
