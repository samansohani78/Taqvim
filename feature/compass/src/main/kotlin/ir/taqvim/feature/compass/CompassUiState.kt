/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** State of the compass screen (T-1302). */
data class CompassUiState(
    val content: CompassContent = CompassContent.Loading,
    /** Stopped mode: the dial keeps its last heading while the sensors keep running. */
    val frozen: Boolean = false,
    /** Whether the Sun's path over the next 24 hours is drawn. */
    val showSunPath: Boolean = false,
)

/** What the compass screen shows. */
sealed interface CompassContent {
    /** Preferences or the first sensor reading are not available yet. */
    data object Loading : CompassContent

    /** The device has no usable orientation sensors. */
    data object SensorUnavailable : CompassContent

    /** The dial. */
    data class Dial(
        /** Heading of the device's top edge, true north when [north] is TRUE, 0‥360. */
        val headingDegrees: Float,
        /** [headingDegrees] rounded and written with the language's digits, e.g. "۲۳۴°". */
        val headingText: String,
        val cardinal: Cardinal,
        val north: NorthReference,
        /** The last announced multiple of 15° (TalkBack), with its [announcedText]. */
        val announcedDegrees: Int,
        val announcedText: String,
        val announcedCardinal: Cardinal,
        val accuracy: CompassAccuracy,
        val placeName: String?,
        val qibla: QiblaMarker?,
        val sun: BodyMarker?,
        val moon: BodyMarker?,
        val sunPath: ImmutableList<PathPoint> = persistentListOf(),
    ) : CompassContent
}

/** Whether headings are relative to true north (declination applied) or magnetic north (no place chosen). */
enum class NorthReference {
    TRUE,
    MAGNETIC,
}

/** The eight compass points. */
enum class Cardinal {
    N,
    NE,
    E,
    SE,
    S,
    SW,
    W,
    NW,
    ;

    companion object {
        private const val SECTOR_DEGREES = 45.0

        /** The compass point nearest [degrees]. */
        fun of(degrees: Double): Cardinal =
            entries[((Angles.wrap360(degrees) + SECTOR_DEGREES / 2) / SECTOR_DEGREES).toInt() % entries.size]
    }
}

/** The Qibla direction and the signed turn from the current heading (positive: turn clockwise, to the right). */
data class QiblaMarker(
    val azimuthDegrees: Float,
    val azimuthText: String,
    val turnDegrees: Float,
    val turnText: String,
) {
    /** Whether the device already points at the Qibla (within [ALIGNED_DEGREES]). */
    val aligned: Boolean get() = kotlin.math.abs(turnDegrees) <= ALIGNED_DEGREES

    companion object {
        const val ALIGNED_DEGREES: Float = 2f
    }
}

/** The Sun or the Moon on the dial. */
data class BodyMarker(
    val azimuthDegrees: Float,
    val azimuthText: String,
    val altitudeDegrees: Float,
    val altitudeText: String,
) {
    val aboveHorizon: Boolean get() = altitudeDegrees > 0f
}

/** One hour of the Sun's path above the horizon; [hourText] is the local hour in the language's digits. */
data class PathPoint(
    val azimuthDegrees: Float,
    val altitudeDegrees: Float,
    val hourText: String,
)

/** User actions of the compass screen. */
@Immutable
data class CompassActions(
    val onToggleFrozen: () -> Unit = {},
    val onToggleSunPath: () -> Unit = {},
)
