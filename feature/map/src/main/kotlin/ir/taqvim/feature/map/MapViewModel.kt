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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import kotlinx.coroutines.flow.map
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
 * the time is moved), the flat map or the globe, city markers and picking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    settingsSource: MapSettingsSource,
    outlineSource: WorldOutlineSource,
    magnetic: MagneticModel,
    clock: Clock,
    citySource: MapCitySource = MapCitySource { _, _ -> emptyList() },
    crescentObserver: CrescentObserver = CrescentObserver.DEFAULT,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val layers = MutableStateFlow(MapUiState.DEFAULT_LAYERS)
    private val camera = MutableStateFlow(MapCamera())
    private val viewSize = MutableStateFlow(ViewSize(0f, 0f))
    private val criterion = MutableStateFlow(CrescentCriterion.YALLOP)
    private val chosenInstant = MutableStateFlow<Instant?>(null)
    private val picked = MutableStateFlow<Coordinates?>(null)
    private val latest = MutableStateFlow<MapInputs?>(null)
    private val shownCities = MutableStateFlow<ImmutableList<MapCity>>(persistentListOf())
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
        combine(
            settingsSource.settings(),
            combine(chosenInstant, minuteTicks(clock), ::Pair),
            layers,
            picked,
            criterion,
        ) { settings, (chosen, now), shown, pick, crescent ->
            MapInputs(settings, chosen ?: now, chosen == null, shown, pick, crescent)
        }.distinctUntilChanged()
            .onEach { latest.value = it }
            .mapLatest { builder.build(it) }
            .flowOn(computeDispatcher)
            .onStart<MapComputed?> { emit(null) }

    private val candidates: Flow<List<MapCity>> =
        settingsSource
            .settings()
            .map { it.language.code }
            .distinctUntilChanged()
            .mapLatest { code ->
                runCatching { citySource.citiesByPopulation(code, CityMarkers.CANDIDATES) }.getOrDefault(emptyList())
            }.flowOn(computeDispatcher)
            .onStart { emit(emptyList()) }

    private val markers: Flow<ImmutableList<MapCity>> =
        combine(candidates, layers, camera, viewSize) { cities, shown, view, size ->
            if (MapLayer.CITIES in shown) markersOf(cities, view, size) else persistentListOf()
        }.distinctUntilChanged()
            .onEach { shownCities.value = it }
            .flowOn(computeDispatcher)
            .onStart { emit(persistentListOf()) }

    val uiState: StateFlow<MapUiState> =
        combine(
            outline,
            computed,
            combine(layers, criterion, ::Pair),
            combine(camera, markers, ::Pair),
            latest,
        ) { outlineState, result, (shown, crescent), (view, cities), inputs ->
            MapUiState(
                outline = outlineState,
                layers = shown,
                viewport = view.viewport,
                projection = view.projection,
                globe = view.globe,
                crescentCriterion = crescent,
                time = result?.time,
                overlays = result?.overlays ?: MapOverlays(),
                cities = cities,
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
        camera.update {
            when (it.projection) {
                MapProjection.FLAT -> it.copy(viewport = it.viewport.zoomedBy(factor, focus, size))
                MapProjection.GLOBE -> it.copy(globe = it.globe.zoomedBy(factor))
            }
        }
    }

    /** Pans the flat map, or turns the globe, with a drag of ([dx], [dy]) pixels. */
    fun onPan(
        dx: Float,
        dy: Float,
        size: ViewSize,
    ) {
        camera.update {
            when (it.projection) {
                MapProjection.FLAT -> it.copy(viewport = it.viewport.pannedBy(dx, dy, size))
                MapProjection.GLOBE -> it.copy(globe = it.globe.rotatedBy(dx, dy, size))
            }
        }
    }

    fun onResize(size: ViewSize) {
        viewSize.value = size
        camera.update { it.copy(viewport = it.viewport.clamped(size)) }
    }

    /** Picks the city marker under [point] when the cities layer is on, otherwise the place under it. */
    fun onPick(
        point: ScreenPoint,
        size: ViewSize,
    ) {
        if (size.isEmpty) return
        val view = camera.value
        val city =
            if (MapLayer.CITIES in layers.value) {
                val radius = CityMarkers.TOUCH_DP * viewSize.value.density
                CityMarkers.hit(shownCities.value, point, radius) { view.toScreen(it, size) }
            } else {
                null
            }
        if (city != null) {
            pick(city.coordinates, city)
        } else {
            view.fromScreen(point, size)?.let { pick(it) }
        }
    }

    /** Picks the place at the view's center (the accessible alternative to tapping). */
    fun onPickCenter() {
        pick(camera.value.center())
    }

    /** Picks [city] as a tap on its marker does (the accessibility action of a shown marker). */
    fun onPickCity(city: MapCity) {
        pick(city.coordinates, city)
    }

    /** Shows the flat map or the globe; the globe first opens over the chosen place. */
    fun onSelectProjection(projection: MapProjection) {
        val place = latest.value?.settings?.place
        camera.update { view ->
            val globe =
                if (projection == MapProjection.GLOBE && view.globe == GlobeView() && place != null) {
                    GlobeView(place.latitude, place.longitude)
                } else {
                    view.globe
                }
            view.copy(projection = projection, globe = globe)
        }
    }

    fun onSelectCrescentCriterion(selected: CrescentCriterion) {
        criterion.value = selected
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

    private fun pick(
        coordinates: Coordinates,
        city: MapCity? = null,
    ) {
        picked.value = coordinates
        effectChannel.trySend(MapEffect.LocationPicked(coordinates, city))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MINUTES_PER_HOUR = 60
        const val LAST_MINUTE = 1_439
        const val MILLIS_PER_MINUTE = 60_000L

        fun markersOf(
            cities: List<MapCity>,
            view: MapCamera,
            size: ViewSize,
        ): ImmutableList<MapCity> {
            val zoom = if (view.projection == MapProjection.GLOBE) view.globe.zoom else view.viewport.zoom
            val spacing = CityMarkers.SPACING_DP * size.density
            return CityMarkers
                .select(cities, size, spacing, CityMarkers.maxCount(zoom)) { view.toScreen(it, size) }
                .toImmutableList()
        }

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
