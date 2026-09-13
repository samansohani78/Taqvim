/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.i18n.NumeralSystem
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The bubble level and ruler (T-1303): orientation classification, a calibrated two-axis (flat) or one-axis (upright)
 * reading, a per-orientation calibration offset persisted through [LevelCalibrationStore], and the ruler unit.
 */
class LevelViewModel(
    settingsSource: CompassSettingsSource,
    sensors: MotionSensors,
    private val calibrationStore: LevelCalibrationStore,
) : ViewModel() {
    private val tab = MutableStateFlow(LevelTab.LEVEL)
    private val unit = MutableStateFlow(RulerUnit.CENTIMETERS)
    private val latestSample = MutableStateFlow<LevelSample?>(null)
    private val latestCalibration = MutableStateFlow(LevelCalibration())

    private val samples: Flow<LevelSample?> =
        sensors
            .gravity()
            .scan<GravitySample, LevelSample?>(null) { previous, sample ->
                when (sample) {
                    GravitySample.Unavailable -> LevelSample.Unavailable
                    is GravitySample.Reading -> measured(previous, sample.gravity)
                }
            }.onEach { latestSample.value = it }

    private val numerals: Flow<NumeralSystem> =
        settingsSource
            .settings()
            .map { it.language.numerals }
            .onStart { emit(NumeralSystem.LATIN) }

    private val calibration: Flow<LevelCalibration> =
        calibrationStore.calibration().onEach { latestCalibration.value = it }.onStart { emit(LevelCalibration()) }

    val uiState: StateFlow<LevelUiState> =
        combine(samples, calibration, numerals, tab, unit) { sample, stored, digits, selectedTab, rulerUnit ->
            LevelUiState(selectedTab, LevelStateMapper.content(sample, stored, digits), rulerUnit, digits)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LevelUiState())

    fun onSelectTab(selected: LevelTab) {
        tab.value = selected
    }

    fun onSelectUnit(selected: RulerUnit) {
        unit.value = selected
    }

    /** Stores the current raw tilt as the zero of the current orientation. */
    fun onCalibrate() {
        val sample = latestSample.value as? LevelSample.Measured ?: return
        val offsets =
            latestCalibration.value.offsets
                .toPersistentMap()
                .putting(sample.orientation, sample.rawTilt)
        save(LevelCalibration(offsets))
    }

    /** Removes the stored offset of the current orientation. */
    fun onResetCalibration() {
        val sample = latestSample.value as? LevelSample.Measured ?: return
        save(
            LevelCalibration(
                latestCalibration.value.offsets
                    .toPersistentMap()
                    .removing(sample.orientation),
            ),
        )
    }

    private fun save(calibration: LevelCalibration) {
        latestCalibration.value = calibration
        viewModelScope.launch { calibrationStore.save(calibration) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Smooths accelerometer readings towards gravity; a gravity sensor is already smooth. */
        const val GRAVITY_ALPHA = 0.2

        fun measured(
            previous: LevelSample?,
            gravity: Vector3,
        ): LevelSample {
            val filtered = gravity.lowPass((previous as? LevelSample.Measured)?.gravity, GRAVITY_ALPHA)
            val orientation = LevelMath.classify(filtered)
            return orientation?.let { LevelSample.Measured(filtered, it) } ?: previous ?: LevelSample.Unavailable
        }
    }
}
