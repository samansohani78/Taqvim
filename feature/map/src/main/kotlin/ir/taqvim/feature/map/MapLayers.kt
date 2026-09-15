/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable
import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.CrescentVisibilityClass
import ir.taqvim.core.astronomy.Houses
import ir.taqvim.core.astronomy.Odeh
import ir.taqvim.core.astronomy.OdehZone
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Yallop
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.NoaaSolarCalculator
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Instant

/** Layers of the world map (T-1301). */
enum class MapLayer {
    DAY_NIGHT,
    MOON_VISIBILITY,
    CRESCENT_VISIBILITY,
    MAGNETIC_DECLINATION,
    MAGNETIC_INCLINATION,
    MAGNETIC_INTENSITY,
    GRID,
    TIME_ZONES,
    TECTONIC_PLATES,
    CITIES,
    QIBLA,
    DIRECT_PATH,
}

/** Daylight by the Sun's altitude: day above −0.833° (sunset), then civil, nautical and astronomical twilight. */
enum class Illumination {
    DAY,
    CIVIL_TWILIGHT,
    NAUTICAL_TWILIGHT,
    ASTRONOMICAL_TWILIGHT,
    NIGHT,
    ;

    companion object {
        private val LIMITS = listOf(-0.833, -6.0, -12.0, -18.0)

        fun of(altitudeDegrees: Double): Illumination =
            entries[LIMITS.indexOfFirst { altitudeDegrees > it }.takeIf { it >= 0 } ?: LIMITS.size]
    }
}

/**
 * Integer values on a [columns] × [rows] grid covering the whole map; cell (column, row) is centered at longitude
 * −180° + (column + ½)·360°/columns and latitude 90° − (row + ½)·180°/rows; [NONE] marks cells without a value.
 */
@Immutable
class ShadeGrid private constructor(
    val columns: Int,
    val rows: Int,
    private val values: IntArray,
) {
    operator fun get(
        column: Int,
        row: Int,
    ): Int = values[row * columns + column]

    override fun equals(other: Any?): Boolean =
        other is ShadeGrid &&
            columns == other.columns &&
            rows == other.rows &&
            values.contentEquals(other.values)

    override fun hashCode(): Int = HASH * (HASH * columns + rows) + values.contentHashCode()

    companion object {
        const val NONE: Int = Int.MIN_VALUE
        private const val HASH = 31
        private const val HALF = 0.5
        private const val HALF_TURN = 180.0
        private const val FULL_TURN = 360.0
        private const val RIGHT_ANGLE = 90.0

        fun latitudeOf(
            row: Int,
            rows: Int,
        ): Double = RIGHT_ANGLE - (row + HALF) * HALF_TURN / rows

        fun longitudeOf(
            column: Int,
            columns: Int,
        ): Double = -HALF_TURN + (column + HALF) * FULL_TURN / columns

        /** A grid whose cells hold [value] of each cell center's latitude and longitude. */
        fun build(
            columns: Int,
            rows: Int,
            value: (latitude: Double, longitude: Double) -> Int,
        ): ShadeGrid {
            require(columns > 0 && rows > 0) { "grid must have cells (was $columns × $rows)" }
            val values =
                IntArray(columns * rows) { index ->
                    value(latitudeOf(index / columns, rows), longitudeOf(index % columns, columns))
                }
            return ShadeGrid(columns, rows, values)
        }
    }
}

/** Where the Sun and the Moon are overhead, and the altitude of a body seen from anywhere given that point. */
object SubPoints {
    private const val MINUTES_PER_DEGREE = 4.0
    private const val NOON_MINUTES = 720.0
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val HALF_TURN = 180.0
    private val GREENWICH = Coordinates(0.0, 0.0)

    /** The subsolar point (A-09): latitude = declination, longitude where apparent solar time is noon. */
    fun sun(instant: Instant): Coordinates {
        val parameters = NoaaSolarCalculator.parameters(NoaaSolarCalculator.julianDay(instant))
        val utcMinutes = instant.toEpochMilliseconds().mod(MILLIS_PER_DAY) / MILLIS_PER_MINUTE
        val longitude = (NOON_MINUTES - utcMinutes - parameters.equationOfTimeMinutes) / MINUTES_PER_DEGREE
        return Coordinates(parameters.declinationDegrees, Equirectangular.wrapLongitude(longitude))
    }

    /** The sublunar point (A-13, A-14): the Moon's geocentric right ascension less apparent sidereal time. */
    fun moon(instant: Instant): Coordinates {
        val ecliptic = Sky.eclipticPosition(CelestialBody.MOON, instant)
        val obliquity = radians(Houses.obliquityDegrees(instant))
        val lambda = radians(ecliptic.longitudeDegrees)
        val beta = radians(ecliptic.latitudeDegrees)
        val rightAscension = degrees(atan2(sin(lambda) * cos(obliquity) - tan(beta) * sin(obliquity), cos(lambda)))
        val declination = degrees(asin(sin(beta) * cos(obliquity) + cos(beta) * sin(obliquity) * sin(lambda)))
        val sidereal = Houses.ramcDegrees(instant, GREENWICH)
        return Coordinates(declination, Equirectangular.wrapLongitude(rightAscension - sidereal))
    }

    /** Altitude in degrees (geometric, no refraction) of a body overhead at [subPoint], seen from a place. */
    fun altitudeDegrees(
        subPoint: Coordinates,
        latitude: Double,
        longitude: Double,
    ): Double {
        val phi = radians(latitude)
        val delta = radians(subPoint.latitude)
        val sine = sin(phi) * sin(delta) + cos(phi) * cos(delta) * cos(radians(longitude - subPoint.longitude))
        return degrees(asin(sine.coerceIn(-1.0, 1.0)))
    }

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}

/** The published crescent visibility criteria the map can show. */
enum class CrescentCriterion {
    /** Yallop (A-06): classes A–F ([CrescentVisibilityClass]). */
    YALLOP,

    /** Odeh (2004): zones A–D ([OdehZone]). */
    ODEH,
}

/** The crescent seen on the first evening after an instant at a place under a criterion. */
fun interface CrescentObserver {
    /** The class ordinal under [criterion] ([CrescentVisibilityClass] or [OdehZone]), `null` when none can be seen. */
    fun visibility(
        place: Coordinates,
        from: Instant,
        criterion: CrescentCriterion,
    ): Int?

    companion object {
        /** Yallop's classes and Odeh's zones from `:core:astronomy`. */
        val DEFAULT: CrescentObserver =
            CrescentObserver { place, from, criterion ->
                when (criterion) {
                    CrescentCriterion.YALLOP -> Yallop.evening(place, from)?.visibility?.ordinal
                    CrescentCriterion.ODEH -> Odeh.evening(place, from)?.zone?.ordinal
                }
            }
    }
}

/** The shaded layers' grids: pure functions of the time, computed off the main thread by the ViewModel. */
object LayerGrids {
    const val SHADE_COLUMNS: Int = 180
    const val SHADE_ROWS: Int = 90
    const val DECLINATION_COLUMNS: Int = 72
    const val DECLINATION_ROWS: Int = 36
    const val CRESCENT_COLUMNS: Int = 36
    const val CRESCENT_ROWS: Int = 18

    /** Day/night at [sun] (the subsolar point): ordinals of [Illumination]. */
    fun illumination(
        sun: Coordinates,
        columns: Int = SHADE_COLUMNS,
        rows: Int = SHADE_ROWS,
    ): ShadeGrid =
        ShadeGrid.build(columns, rows) { latitude, longitude ->
            Illumination.of(SubPoints.altitudeDegrees(sun, latitude, longitude)).ordinal
        }

    /** 1 where the Moon (overhead at [moon]) is above the horizon, else 0. */
    fun moonVisibility(
        moon: Coordinates,
        columns: Int = SHADE_COLUMNS,
        rows: Int = SHADE_ROWS,
    ): ShadeGrid =
        ShadeGrid.build(columns, rows) { latitude, longitude ->
            if (SubPoints.altitudeDegrees(moon, latitude, longitude) > 0) 1 else 0
        }

    /** Magnetic declination rounded to whole degrees (positive east), [ShadeGrid.NONE] where the model has none. */
    fun declination(
        model: MagneticModel,
        instant: Instant,
        columns: Int = DECLINATION_COLUMNS,
        rows: Int = DECLINATION_ROWS,
    ): ShadeGrid = magnetic(model, instant, columns, rows) { it.declinationDegrees }

    /** Magnetic inclination rounded to whole degrees (positive downward), [ShadeGrid.NONE] where the model has none. */
    fun inclination(
        model: MagneticModel,
        instant: Instant,
        columns: Int = DECLINATION_COLUMNS,
        rows: Int = DECLINATION_ROWS,
    ): ShadeGrid = magnetic(model, instant, columns, rows) { it.inclinationDegrees }

    /** Total field strength rounded to whole nanotesla, [ShadeGrid.NONE] where the model has none. */
    fun intensity(
        model: MagneticModel,
        instant: Instant,
        columns: Int = DECLINATION_COLUMNS,
        rows: Int = DECLINATION_ROWS,
    ): ShadeGrid = magnetic(model, instant, columns, rows) { it.fieldStrengthNanotesla }

    /**
     * Crescent class ordinals under [criterion] of the first evening after [from], [ShadeGrid.NONE] where no crescent
     * can be seen.
     */
    fun crescent(
        from: Instant,
        observer: CrescentObserver = CrescentObserver.DEFAULT,
        criterion: CrescentCriterion = CrescentCriterion.YALLOP,
        columns: Int = CRESCENT_COLUMNS,
        rows: Int = CRESCENT_ROWS,
    ): ShadeGrid =
        ShadeGrid.build(columns, rows) { latitude, longitude ->
            observer.visibility(Coordinates(latitude, longitude), from, criterion) ?: ShadeGrid.NONE
        }

    private fun magnetic(
        model: MagneticModel,
        instant: Instant,
        columns: Int,
        rows: Int,
        element: (MagneticElements) -> Double,
    ): ShadeGrid =
        ShadeGrid.build(columns, rows) { latitude, longitude ->
            element(model.elements(Coordinates(latitude, longitude), instant))
                .takeIf { it.isFinite() }
                ?.roundToInt() ?: ShadeGrid.NONE
        }
}
