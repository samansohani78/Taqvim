/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.compose.runtime.Immutable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import kotlin.math.abs
import kotlin.math.roundToInt

/** State of the bubble level and ruler (T-1303). */
data class LevelUiState(
    val tab: LevelTab = LevelTab.LEVEL,
    val content: LevelContent = LevelContent.Loading,
    val rulerUnit: RulerUnit = RulerUnit.CENTIMETERS,
    /** Digits of angles and ruler labels. */
    val numerals: NumeralSystem = NumeralSystem.LATIN,
)

/** The two tabs of the level screen. */
enum class LevelTab {
    LEVEL,
    RULER,
}

/** What the level tab shows. */
sealed interface LevelContent {
    data object Loading : LevelContent

    /** The device has no gravity sensor or accelerometer. */
    data object SensorUnavailable : LevelContent

    /** A calibrated reading. */
    data class Reading(
        val orientation: DeviceOrientation,
        val x: Float,
        val y: Float,
        /** |x| and |y| with one decimal and the language's digits. */
        val xText: String,
        val yText: String,
        val isLevel: Boolean,
        /** Whether a calibration offset is stored for [orientation]. */
        val calibrated: Boolean,
    ) : LevelContent
}

/** User actions of the level screen. */
@Immutable
data class LevelActions(
    val onSelectTab: (LevelTab) -> Unit = {},
    val onCalibrate: () -> Unit = {},
    val onResetCalibration: () -> Unit = {},
    val onSelectUnit: (RulerUnit) -> Unit = {},
)

/** A measured gravity vector and the orientation it implies. */
sealed interface LevelSample {
    data class Measured(
        val gravity: Vector3,
        val orientation: DeviceOrientation,
    ) : LevelSample {
        val rawTilt: Tilt get() = LevelMath.tilt(gravity, orientation)
    }

    data object Unavailable : LevelSample
}

/** Pure mapping to [LevelContent]. */
object LevelStateMapper {
    private const val TENTHS = 10.0

    fun content(
        sample: LevelSample?,
        calibration: LevelCalibration,
        numerals: NumeralSystem,
    ): LevelContent =
        when (sample) {
            null -> {
                LevelContent.Loading
            }

            LevelSample.Unavailable -> {
                LevelContent.SensorUnavailable
            }

            is LevelSample.Measured -> {
                val tilt = LevelMath.calibrated(sample.rawTilt, sample.orientation, calibration)
                LevelContent.Reading(
                    orientation = sample.orientation,
                    x = tilt.x.toFloat(),
                    y = tilt.y.toFloat(),
                    xText = oneDecimal(tilt.x, numerals),
                    yText = oneDecimal(tilt.y, numerals),
                    isLevel = LevelMath.isLevel(tilt, sample.orientation),
                    calibrated = sample.orientation in calibration.offsets,
                )
            }
        }

    /** |[value]| with one decimal, e.g. "1.2" → "۱٫۲" for Persian digits. */
    internal fun oneDecimal(
        value: Double,
        numerals: NumeralSystem,
    ): String {
        val tenths = (abs(value) * TENTHS).roundToInt()
        val text = "${tenths / TENTHS.toInt()}${numerals.decimalSeparator}${tenths % TENTHS.toInt()}"
        return Numerals.localizeDigits(text, numerals)
    }
}
