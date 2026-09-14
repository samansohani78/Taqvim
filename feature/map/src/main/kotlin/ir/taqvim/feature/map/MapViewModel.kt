/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Coordinates
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The world map (T-1301): outline, layers computed off the main thread for the shown moment (following the clock until
 * the time is moved), viewport and picking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    settingsSource: MapSettingsSource,
    outlineSource: WorldOutlineSource,
    magnetic: MagneticModel,
    clock: Clock,
    crescentObserver: CrescentObserver = CrescentObserver.YALLOP,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val layers = MutableStateFlow(MapUiState.DEFAULT_LAYERS)
    private val viewport = MutableStateFlow(MapViewport())
    private val chosenInstant = MutableStateFlow<Instant?>(null)
    private val picked = MutableStateFlow<Coordinates?>(null)
    private val latest = MutableStateFlow<MapInputs?>(null)
    private val effectChannel = Channel<MapEffect>(Channel.BUFFERED)
    private val builder = MapOverlayBuilder(magnetic, crescentObserver)

    /** One-off events: picked locations. */
    val effects: Flow<MapEffect> = effectChannel.receiveAsFlow()

    private val outline: Flow<OutlineState> =
        flow {
            emit(OutlineState.Loading)
            emit(runCatching { outlineSource.load() }.fold({ OutlineState.Ready(it) }, { OutlineState.Unavailable }))
        }.flowOn(computeDispatcher)

    private val computed: Flow<MapComputed?> =
        combine(settingsSource.settings(), combine(chosenInstant, minuteTicks(clock), ::Pair), layers, picked) {
            settings,
            (chosen, now),
            shown,
            pick,
            ->
            MapInputs(settings, chosen ?: now, chosen == null, shown, pick)
        }.distinctUntilChanged()
            .onEach { latest.value = it }
            .mapLatest { builder.build(it) }
            .flowOn(computeDispatcher)
            .onStart<MapComputed?> { emit(null) }

    val uiState: StateFlow<MapUiState> =
        combine(outline, computed, layers, viewport, latest) { outlineState, result, shown, view, inputs ->
            MapUiState(
                outline = outlineState,
                layers = shown,
                viewport = view,
                time = result?.time,
                overlays = result?.overlays ?: MapOverlays(),
                picked = result?.picked,
                hasPlace = inputs?.settings?.place != null,
            )
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapUiState())

    fun onToggleLayer(layer: MapLayer) {
        layers.update { shown -> (if (layer in shown) shown - layer else shown + layer).toPersistentSet() }
    }

    fun onZoom(
        factor: Double,
        focus: ScreenPoint,
        size: ViewSize,
    ) {
        viewport.update { it.zoomedBy(factor, focus, size) }
    }

    fun onPan(
        dx: Float,
        dy: Float,
        size: ViewSize,
    ) {
        viewport.update { it.pannedBy(dx, dy, size) }
    }

    fun onResize(size: ViewSize) {
        viewport.update { it.clamped(size) }
    }

    fun onPick(
        point: ScreenPoint,
        size: ViewSize,
    ) {
        if (!size.isEmpty) pick(viewport.value.fromScreen(point, size))
    }

    /** Picks the map point at the view's center (the accessible alternative to tapping). */
    fun onPickCenter() {
        viewport.value.let { pick(MapPoint(it.centerX, it.centerY)) }
    }

    /** Shows [minute] after local midnight of the shown day. */
    fun onSelectMinute(minute: Int) {
        val inputs = latest.value ?: return
        val zone = inputs.settings.timeZone
        val date = inputs.instant.toLocalDateTime(zone).date
        val clamped = minute.coerceIn(0, LAST_MINUTE)
        val time = LocalTime(clamped / MINUTES_PER_HOUR, clamped % MINUTES_PER_HOUR)
        chosenInstant.value = date.atTime(time).toInstant(zone)
    }

    /** Moves the shown moment by [days] days. */
    fun onStepDay(days: Int) {
        val inputs = latest.value ?: return
        chosenInstant.value = inputs.instant + days.days
    }

    /** Follows the clock again. */
    fun onNow() {
        chosenInstant.value = null
    }

    private fun pick(point: MapPoint) {
        val coordinates = Equirectangular.unproject(MapPoint(point.x.coerceIn(0.0, 1.0), point.y.coerceIn(0.0, 1.0)))
        picked.value = coordinates
        effectChannel.trySend(MapEffect.LocationPicked(coordinates))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MINUTES_PER_HOUR = 60
        const val LAST_MINUTE = 1_439
        const val MILLIS_PER_MINUTE = 60_000L

        /** [clock]'s time now and then at every following minute boundary. */
        fun minuteTicks(clock: Clock): Flow<Instant> =
            flow {
                while (true) {
                    val now = clock.now()
                    emit(now)
                    delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
                }
            }
    }
}
