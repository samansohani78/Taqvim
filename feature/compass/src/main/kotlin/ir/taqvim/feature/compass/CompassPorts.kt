/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Coordinates
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** What the compass and the level need from the preferences and the chosen place (T-600, T-603); bound in `:app`. */
data class CompassSettings(
    /** App language: digits of angles and hours. */
    val language: LanguageSpec,
    /** The chosen place, or `null` when none has been chosen (no Qibla, no true north, no Sun/Moon markers). */
    val place: CompassPlace?,
)

/** A chosen place with its localized [name] and civil [timeZone] (hours of the Sun's path). */
data class CompassPlace(
    val name: String,
    val coordinates: Coordinates,
    val timeZone: TimeZone,
)

/** The current [CompassSettings]; emits nothing until preferences have loaded. */
fun interface CompassSettingsSource {
    fun settings(): Flow<CompassSettings>
}

/** Per-orientation zero offsets of the level (T-1303); persisted by `:app`. */
data class LevelCalibration(
    val offsets: ImmutableMap<DeviceOrientation, Tilt> = persistentMapOf(),
)

/** Stored [LevelCalibration] of the bubble level. */
interface LevelCalibrationStore {
    fun calibration(): Flow<LevelCalibration>

    suspend fun save(calibration: LevelCalibration)
}
