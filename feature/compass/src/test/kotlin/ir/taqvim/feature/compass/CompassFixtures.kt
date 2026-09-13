/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.compose.runtime.Composable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.TimeZones
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Sample places, instants and sensor readings for the compass and level tests (sample coordinates, not data). */
object CompassFixtures {
    val TEHRAN: CompassPlace = CompassPlace("Tehran", Coordinates(35.69, 51.39), TimeZones.TEHRAN)

    /** 12:00 in Tehran on the June solstice of 2026. */
    val NOON: Instant = Instant.parse("2026-06-21T08:30:00Z")

    /** A fixed declination of 5° east, so expectations do not depend on the magnetic model. */
    val FIVE_EAST: DeclinationModel = DeclinationModel { _, _ -> 5.0 }

    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    fun settings(
        languageCode: String = "en",
        place: CompassPlace? = TEHRAN,
    ): CompassSettings = CompassSettings(language(languageCode), place)

    /** The rotation of a device lying flat whose top edge points at magnetic [heading] degrees. */
    fun flatHeading(heading: Double): RotationMatrix {
        val rotation = -Angles.delta(0.0, heading) * PI / 180.0
        return RotationMath.fromRotationVector(Vector3(0.0, 0.0, sin(rotation / 2)))
    }

    fun reading(
        heading: Double,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
    ): OrientationSample = OrientationSample.Reading(flatHeading(heading), accuracy)

    /** A dial state at Tehran noon for a magnetic [heading]. */
    fun dial(
        languageCode: String = "en",
        heading: Double = 40.0,
        place: CompassPlace? = TEHRAN,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        showSunPath: Boolean = false,
    ): CompassUiState {
        val settings = settings(languageCode, place)
        val sky = place?.let { CompassStateMapper.snapshot(it, NOON, FIVE_EAST) }
        val measured = HeadingReading.Measured(heading, accuracy)
        return CompassStateMapper.map(settings, measured, sky, null, CompassUiState(showSunPath = showSunPath))
    }
}

/** [MotionSensors] driven by the test. */
class FakeSensors(
    private val orientation: Flow<OrientationSample> = emptyFlow(),
    private val gravity: Flow<GravitySample> = emptyFlow(),
) : MotionSensors {
    override fun orientation(): Flow<OrientationSample> = orientation

    override fun gravity(): Flow<GravitySample> = gravity
}

/** An in-memory [LevelCalibrationStore]. */
class FakeCalibrationStore : LevelCalibrationStore {
    val stored = MutableStateFlow(LevelCalibration())

    override fun calibration(): Flow<LevelCalibration> = stored

    override suspend fun save(calibration: LevelCalibration) {
        stored.value = calibration
    }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun CompassTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
