/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Coordinates
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId

/** Parsing and display of typed coordinates (T-1502). */
object CoordinateInput {
    /** Decimal places shown for coordinates (about 11 m). */
    const val DISPLAY_DECIMALS: Int = 4

    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0
    private const val ARABIC_COMMA = 0x060C

    /** [text] as a latitude in degrees, written with digits of any supported script; `null` when invalid. */
    fun latitude(text: String): Double? = degrees(text, MAX_LATITUDE)

    /** [text] as a longitude in degrees, written with digits of any supported script; `null` when invalid. */
    fun longitude(text: String): Double? = degrees(text, MAX_LONGITUDE)

    /** Both fields as [Coordinates], or `null` when either is invalid. */
    fun coordinates(
        latitudeText: String,
        longitudeText: String,
    ): Coordinates? {
        val latitude = latitude(latitudeText) ?: return null
        val longitude = longitude(longitudeText) ?: return null
        return Coordinates(latitude, longitude)
    }

    /** [coordinates] as "latitude, longitude" with [DISPLAY_DECIMALS] decimals in [numerals]. */
    fun format(
        coordinates: Coordinates,
        numerals: NumeralSystem,
    ): String = decimal(coordinates.latitude, numerals) + separator(numerals) + decimal(coordinates.longitude, numerals)

    /** The list separator between latitude and longitude: the Arabic comma where the digits are Arabic-script. */
    fun separator(numerals: NumeralSystem): String =
        if (numerals == NumeralSystem.PERSIAN || numerals == NumeralSystem.EASTERN_ARABIC) {
            Char(ARABIC_COMMA) + " "
        } else {
            ", "
        }

    /** [value] with [DISPLAY_DECIMALS] decimals in [numerals]. */
    fun decimal(
        value: Double,
        numerals: NumeralSystem,
    ): String = Numerals.format(BigDecimal.valueOf(value).setScale(DISPLAY_DECIMALS, RoundingMode.HALF_UP), numerals)

    private fun degrees(
        text: String,
        limit: Double,
    ): Double? =
        Numerals
            .parseDecimal(text)
            ?.toDouble()
            ?.takeIf { it.isFinite() && it in -limit..limit }
}

/** [TimeZoneIds] of the platform's time zone database. */
object PlatformTimeZoneIds : TimeZoneIds {
    override fun isKnown(id: String): Boolean = id in ZoneId.getAvailableZoneIds()
}
