/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Orientation of the Moon's bright limb (Meeus, Astronomical Algorithms, eqs. 48.5 and 14.1); all angles in degrees. */
internal object LunarLimb {
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    /**
     * Position angle of the Moon's bright limb (eq. 48.5), counted from celestial north through east, in 0°‥360°, from
     * the equatorial coordinates of date of the Sun ([sunRightAscension], [sunDeclination]) and the Moon.
     */
    fun brightLimbPositionAngle(
        sunRightAscension: Double,
        sunDeclination: Double,
        moonRightAscension: Double,
        moonDeclination: Double,
    ): Double {
        val sunDec = Math.toRadians(sunDeclination)
        val moonDec = Math.toRadians(moonDeclination)
        val difference = Math.toRadians(sunRightAscension - moonRightAscension)
        val angle =
            atan2(
                cos(sunDec) * sin(difference),
                sin(sunDec) * cos(moonDec) - cos(sunDec) * sin(moonDec) * cos(difference),
            )
        return Math.toDegrees(angle).mod(FULL_TURN)
    }

    /**
     * Parallactic angle (eq. 14.1) of a body at local [hourAngle] and [declination] seen from [latitude]: the position
     * angle of the zenith direction, negative east of the meridian, in −180°‥180°.
     */
    fun parallacticAngle(
        hourAngle: Double,
        latitude: Double,
        declination: Double,
    ): Double {
        val h = Math.toRadians(hourAngle)
        val dec = Math.toRadians(declination)
        val angle = atan2(sin(h), tan(Math.toRadians(latitude)) * cos(dec) - sin(dec) * cos(h))
        return Math.toDegrees(angle)
    }

    /** [angle] wrapped into −180°‥180° (−180° itself becomes 180°). */
    fun signed(angle: Double): Double {
        val wrapped = (angle + HALF_TURN).mod(FULL_TURN) - HALF_TURN
        return if (wrapped <= -HALF_TURN) wrapped + FULL_TURN else wrapped
    }
}
