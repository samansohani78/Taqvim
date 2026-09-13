/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Qibla
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.SkyPosition
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toLocalDateTime

/** The filtered sensor heading shown on the dial. */
sealed interface HeadingReading {
    data class Measured(
        /** Magnetic heading after low-pass filtering, 0‥360. */
        val magneticDegrees: Double,
        val accuracy: CompassAccuracy,
    ) : HeadingReading

    data object Unavailable : HeadingReading
}

/** Place-dependent values that change slowly; recomputed once a minute. */
data class SkySnapshot(
    val declinationDegrees: Double,
    /** Qibla bearing from true north; `null` at the Kaaba or its antipode. */
    val qiblaDegrees: Double?,
    val sun: SkyPosition,
    val moon: SkyPosition,
    /** The Sun at each whole hour of the next 24 hours, above the horizon only. */
    val sunPath: List<Pair<Instant, SkyPosition>>,
)

/** Pure mapping from settings, sensor heading and sky snapshot to [CompassUiState] (T-1302). */
object CompassStateMapper {
    private const val PATH_HOURS = 24
    private const val HOUR_DIGITS = 2

    /** The [SkySnapshot] of [place] at [instant]. */
    fun snapshot(
        place: CompassPlace,
        instant: Instant,
        declination: DeclinationModel,
    ): SkySnapshot {
        val coordinates = place.coordinates
        val firstHour = Instant.fromEpochSeconds(instant.epochSeconds - instant.epochSeconds.mod(SECONDS_PER_HOUR))
        val path =
            (1..PATH_HOURS)
                .map { firstHour + it.hours }
                .map { it to Sky.skyPosition(CelestialBody.SUN, it, coordinates) }
                .filter { (_, position) -> position.altitudeDegrees > 0.0 }
        return SkySnapshot(
            declinationDegrees = declination.declinationDegrees(coordinates, instant),
            qiblaDegrees = Qibla.bearingDegrees(coordinates),
            sun = Sky.skyPosition(CelestialBody.SUN, instant, coordinates),
            moon = Sky.skyPosition(CelestialBody.MOON, instant, coordinates),
            sunPath = path,
        )
    }

    /** The screen state; [announced] is the TalkBack bucket from [HeadingAnnouncements]. */
    fun map(
        settings: CompassSettings?,
        heading: HeadingReading?,
        sky: SkySnapshot?,
        announced: Int?,
        flags: CompassUiState,
    ): CompassUiState {
        val content =
            when {
                heading == HeadingReading.Unavailable -> CompassContent.SensorUnavailable
                settings == null || heading !is HeadingReading.Measured -> CompassContent.Loading
                else -> dial(settings, heading, sky.takeIf { settings.place != null }, announced)
            }
        return flags.copy(content = content)
    }

    private fun dial(
        settings: CompassSettings,
        heading: HeadingReading.Measured,
        sky: SkySnapshot?,
        announced: Int?,
    ): CompassContent.Dial {
        val numerals = settings.language.numerals
        val degrees = Angles.wrap360(heading.magneticDegrees + (sky?.declinationDegrees ?: 0.0))
        val bucket = announced ?: HeadingAnnouncements.next(null, degrees)
        return CompassContent.Dial(
            headingDegrees = degrees.toFloat(),
            headingText = degreesText(degrees, numerals),
            cardinal = Cardinal.of(degrees),
            north = if (sky == null) NorthReference.MAGNETIC else NorthReference.TRUE,
            announcedDegrees = bucket,
            announcedText = Numerals.localizeDigits(bucket.toString(), numerals),
            announcedCardinal = Cardinal.of(bucket.toDouble()),
            accuracy = heading.accuracy,
            placeName = settings.place?.name,
            qibla = sky?.qiblaDegrees?.let { qibla(it, degrees, numerals) },
            sun = sky?.sun?.let { body(it, numerals) },
            moon = sky?.moon?.let { body(it, numerals) },
            sunPath =
                sky
                    ?.sunPath
                    .orEmpty()
                    .map { (instant, position) -> pathPoint(instant, position, settings, numerals) }
                    .toImmutableList(),
        )
    }

    private fun qibla(
        bearing: Double,
        heading: Double,
        numerals: NumeralSystem,
    ): QiblaMarker {
        val turn = Angles.delta(heading, bearing)
        return QiblaMarker(
            azimuthDegrees = bearing.toFloat(),
            azimuthText = degreesText(bearing, numerals),
            turnDegrees = turn.toFloat(),
            turnText = degreesText(abs(turn), numerals),
        )
    }

    private fun body(
        position: SkyPosition,
        numerals: NumeralSystem,
    ): BodyMarker =
        BodyMarker(
            azimuthDegrees = Angles.wrap360(position.azimuthDegrees).toFloat(),
            azimuthText = degreesText(position.azimuthDegrees, numerals),
            altitudeDegrees = position.altitudeDegrees.toFloat(),
            altitudeText = degreesText(abs(position.altitudeDegrees), numerals),
        )

    private fun pathPoint(
        instant: Instant,
        position: SkyPosition,
        settings: CompassSettings,
        numerals: NumeralSystem,
    ): PathPoint {
        val zone = settings.place?.timeZone ?: kotlinx.datetime.TimeZone.UTC
        val hour =
            instant
                .toLocalDateTime(zone)
                .hour
                .toString()
                .padStart(HOUR_DIGITS, '0')
        return PathPoint(
            position.azimuthDegrees.toFloat(),
            position.altitudeDegrees.toFloat(),
            Numerals.localizeDigits(hour, numerals),
        )
    }

    /** Whole degrees in 0‥359 (or the rounded magnitude for turns and altitudes) with the language's digits. */
    internal fun degreesText(
        degrees: Double,
        numerals: NumeralSystem,
    ): String {
        val rounded = degrees.roundToInt().let { if (degrees >= 0.0 && it == FULL_TURN) 0 else it }
        return Numerals.localizeDigits(rounded.toString(), numerals)
    }

    private const val SECONDS_PER_HOUR = 3_600L
    private const val FULL_TURN = 360
}
