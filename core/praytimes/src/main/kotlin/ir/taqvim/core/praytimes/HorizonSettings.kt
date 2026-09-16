/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import kotlin.math.sqrt

/**
 * The horizon that sunrise and sunset are measured against (A-10, ADR-0029). The Sun's upper limb touches the
 * apparent horizon: its centre is below the geometric horizon by the refraction at the horizon, 34′ for a standard
 * atmosphere, scaled by [pressureHectopascals] and [temperatureCelsius] as in Meeus, *Astronomical Algorithms* 2nd
 * ed., ch. 16 (P/1010 × 283/(273 + T)); by the Sun's semi-diameter, the conventional 16′ or with
 * [useActualSemiDiameter] its value at the Sun's distance (959.63″ at 1 au); and, with [useElevation], by the dip of
 * the horizon for the place's elevation above its surroundings (1.76′ × √metres, the dip formula of *The American
 * Practical Navigator*, NGA Pub. 9). The defaults (34′ + 16′, no dip) reproduce the Institute of Geophysics' official
 * sunrise and sunset. Twilight angles are always below the geometric horizon and ignore these settings.
 */
public data class HorizonSettings(
    public val useElevation: Boolean = false,
    public val useActualSemiDiameter: Boolean = false,
    public val pressureHectopascals: Double = STANDARD_PRESSURE_HPA,
    public val temperatureCelsius: Double = STANDARD_TEMPERATURE_CELSIUS,
) {
    init {
        require(pressureHectopascals.isFinite() && pressureHectopascals >= 0) {
            "pressure must be a finite non-negative number of hectopascals (was $pressureHectopascals)"
        }
        require(temperatureCelsius.isFinite() && temperatureCelsius > ABSOLUTE_ZERO_CELSIUS) {
            "temperature must be above absolute zero (was $temperatureCelsius °C)"
        }
    }

    /** The Sun centre's geometric altitude at sunrise and sunset in degrees, for [distanceAu] and [elevationMeters]. */
    internal fun sunriseAltitude(
        distanceAu: Double,
        elevationMeters: Double,
    ): Double {
        val refraction =
            HORIZON_REFRACTION_ARCMIN * (pressureHectopascals / STANDARD_PRESSURE_HPA) *
                (KELVIN_AT_STANDARD / (CELSIUS_TO_KELVIN + temperatureCelsius))
        val semiDiameter =
            if (useActualSemiDiameter) {
                SOLAR_SEMI_DIAMETER_ARCSEC_AT_1_AU / distanceAu / ARC_SECONDS_PER_MINUTE
            } else {
                MEAN_SEMI_DIAMETER_ARCMIN
            }
        val dip = if (useElevation && elevationMeters > 0) DIP_ARCMIN_PER_SQRT_METRE * sqrt(elevationMeters) else 0.0
        return -(refraction + semiDiameter + dip) / ARC_MINUTES_PER_DEGREE
    }

    public companion object {
        /** Pressure of Meeus's standard atmosphere for refraction, in hectopascals (millibars). */
        public const val STANDARD_PRESSURE_HPA: Double = 1_010.0

        /** Temperature of that standard atmosphere, in degrees Celsius. */
        public const val STANDARD_TEMPERATURE_CELSIUS: Double = 10.0

        private const val HORIZON_REFRACTION_ARCMIN = 34.0
        private const val KELVIN_AT_STANDARD = 283.0
        private const val CELSIUS_TO_KELVIN = 273.0
        private const val ABSOLUTE_ZERO_CELSIUS = -273.15

        /** The Sun's angular semi-diameter at 1 au: 15′ 59.63″, the classical value of the astronomical almanacs. */
        private const val SOLAR_SEMI_DIAMETER_ARCSEC_AT_1_AU = 959.63
        private const val MEAN_SEMI_DIAMETER_ARCMIN = 16.0
        private const val DIP_ARCMIN_PER_SQRT_METRE = 1.76
        private const val ARC_SECONDS_PER_MINUTE = 60.0
        private const val ARC_MINUTES_PER_DEGREE = 60.0
    }
}
