/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The compass (T-1302): the filtered sensor heading (true north once a place is known), Qibla, Sun and Moon markers
 * and the Sun's path, stopped mode, and a TalkBack announcement every 15°.
 */
class CompassViewModel(
    settingsSource: CompassSettingsSource,
    sensors: MotionSensors,
    declination: DeclinationModel,
    clock: Clock,
) : ViewModel() {
    private val frozen = MutableStateFlow(false)
    private val showSunPath = MutableStateFlow(false)
    private val settings = settingsSource.settings().onStart<CompassSettings?> { emit(null) }

    private val shownHeading: Flow<HeadingReading?> =
        combine(filteredHeadings(sensors), frozen) { heading, isFrozen -> heading to isFrozen }
            .scan<Pair<HeadingReading, Boolean>, HeadingReading?>(null) { shown, (heading, isFrozen) ->
                if (isFrozen && shown is HeadingReading.Measured) shown else heading
            }

    private val sky: Flow<SkySnapshot?> =
        combine(settings, minuteTicks(clock)) { current, now ->
            current?.place?.let { CompassStateMapper.snapshot(it, now, declination) }
        }.onStart { emit(null) }

    private val view: Flow<CompassInputs> =
        combine(settings, shownHeading, sky) { current, heading, snapshot -> Triple(current, heading, snapshot) }

    val uiState: StateFlow<CompassUiState> =
        combine(view.withAnnouncements(), frozen, showSunPath) { (triple, announced), isFrozen, path ->
            val (current, heading, snapshot) = triple
            val flags = CompassUiState(frozen = isFrozen, showSunPath = path)
            CompassStateMapper.map(current, heading, snapshot, announced, flags)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CompassUiState())

    fun onToggleFrozen() {
        frozen.update { !it }
    }

    fun onToggleSunPath() {
        showSunPath.update { !it }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Weight of a new heading sample: smooth at the sensor rate, still settling within about a quarter second. */
        const val HEADING_ALPHA = 0.15

        fun filteredHeadings(sensors: MotionSensors): Flow<HeadingReading> =
            sensors
                .orientation()
                .scan<OrientationSample, HeadingReading?>(null) { previous, sample ->
                    when (sample) {
                        OrientationSample.Unavailable -> {
                            HeadingReading.Unavailable
                        }

                        is OrientationSample.Reading -> {
                            val azimuth = RotationMath.angles(sample.rotation).azimuthDegrees
                            val last = (previous as? HeadingReading.Measured)?.magneticDegrees
                            HeadingReading.Measured(Angles.lowPass(last, azimuth, HEADING_ALPHA), sample.accuracy)
                        }
                    }
                }.filterNotNull()

        /** Attaches the TalkBack bucket of each true (or magnetic) heading, with hysteresis across emissions. */
        fun Flow<CompassInputs>.withAnnouncements(): Flow<Pair<CompassInputs, Int?>> =
            scan<CompassInputs, Pair<CompassInputs, Int?>>(Triple(null, null, null) to null) { (_, previous), inputs ->
                val heading = inputs.second as? HeadingReading.Measured
                val announced =
                    heading?.let {
                        val degrees = it.magneticDegrees + (inputs.third?.declinationDegrees ?: 0.0)
                        HeadingAnnouncements.next(previous, degrees)
                    } ?: previous
                inputs to announced
            }
    }
}

/** Settings, shown heading and sky snapshot of one emission. */
private typealias CompassInputs = Triple<CompassSettings?, HeadingReading?, SkySnapshot?>

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
