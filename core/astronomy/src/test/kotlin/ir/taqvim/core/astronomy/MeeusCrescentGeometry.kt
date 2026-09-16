/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.time.Instant

/** Topocentric crescent geometry from the independent model: arcs of vision and light in degrees, width in ′. */
internal data class IndependentCrescent(
    val arcVisionDegrees: Double,
    val arcLightDegrees: Double,
    val widthArcMinutes: Double,
)

/**
 * I08: a second, test-only computation of the crescent geometry from the textbook formulas of Jean Meeus,
 * *Astronomical Algorithms* 2nd ed. — Sun ch. 25 (low accuracy), Moon ch. 47 (the largest terms of Tables 47.A and
 * 47.B), nutation ch. 22 (low accuracy), sidereal time eq. 12.4, topocentric parallax ch. 40 (eq. 40.2–40.3) and
 * altitude eq. 13.6 — with ΔT from the Morrison–Stephenson parabola. It shares no code or ephemeris with the app, so
 * agreement checks the app's geometry rather than repeating it.
 */
internal object MeeusCrescentGeometry {
    private const val JD_UNIX_EPOCH = 2_440_587.5
    private const val JD_J2000 = 2_451_545.0
    private const val DAYS_PER_CENTURY = 36_525.0
    private const val EARTH_RADIUS_KM = 6_378.14
    private const val AU_KM = 149_597_870.7
    private const val MOON_SEMI_DIAMETER_KM_ARCSEC = 358_473_400.0

    /** The crescent seen from [place] at [instant] (airless altitudes). */
    fun at(
        place: Coordinates,
        instant: Instant,
    ): IndependentCrescent {
        val jdUt = JD_UNIX_EPOCH + instant.toEpochMilliseconds() / 86_400_000.0
        val tt = (jdUt + deltaTSeconds(jdUt) / 86_400.0 - JD_J2000) / DAYS_PER_CENTURY
        val nutation = Nutation.at(tt)
        val sun = MeeusSun.at(tt, nutation)
        val moon = MeeusMoon.at(tt, nutation)
        val lst = localSiderealDegrees(jdUt, nutation, place.longitude)
        val sunTopo = topocentric(sun, place, lst, EARTH_RADIUS_KM / (sun.distanceKm))
        val moonTopo = topocentric(moon, place, lst, EARTH_RADIUS_KM / moon.distanceKm)
        val arcLight = separation(sunTopo, moonTopo)
        val moonTopoDistance = moon.distanceKm * moonTopo.distanceScale
        val semiDiameter = MOON_SEMI_DIAMETER_KM_ARCSEC / moonTopoDistance / 60.0
        return IndependentCrescent(
            arcVisionDegrees = altitude(moonTopo, place) - altitude(sunTopo, place),
            arcLightDegrees = arcLight,
            widthArcMinutes = semiDiameter * (1 - cos(rad(arcLight))),
        )
    }

    private data class Equatorial(
        val raDegrees: Double,
        val decDegrees: Double,
        val distanceKm: Double,
    )

    private data class Topocentric(
        val raDegrees: Double,
        val decDegrees: Double,
        val hourAngleDegrees: Double,
        val distanceScale: Double,
    )

    internal data class Nutation(
        val longitudeDegrees: Double,
        val obliquityDegrees: Double,
    ) {
        companion object {
            fun at(t: Double): Nutation {
                val omega = rad(125.04452 - 1934.136261 * t)
                val l = rad(280.4665 + 36000.7698 * t)
                val lm = rad(218.3165 + 481267.8813 * t)
                val dPsi = -17.20 * sin(omega) - 1.32 * sin(2 * l) - 0.23 * sin(2 * lm) + 0.21 * sin(2 * omega)
                val dEps = 9.20 * cos(omega) + 0.57 * cos(2 * l) + 0.10 * cos(2 * lm) - 0.09 * cos(2 * omega)
                val eps0 = 23.0 + 26.0 / 60 + (21.448 - 46.8150 * t - 0.00059 * t * t + 0.001813 * t * t * t) / 3600
                return Nutation(dPsi / 3600, eps0 + dEps / 3600)
            }
        }
    }

    private object MeeusSun {
        fun at(
            t: Double,
            nutation: Nutation,
        ): Equatorial {
            val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
            val m = rad(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
            val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
            val c =
                (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
                    (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
            val nu = m + rad(c)
            val r = 1.000001018 * (1 - e * e) / (1 + e * cos(nu))
            // Apparent longitude: geometric + nutation − aberration (20.4955″ / R).
            val lambda = l0 + c + nutation.longitudeDegrees - 20.4955 / 3600 / r
            return toEquatorial(lambda, 0.0, r * AU_KM, nutation.obliquityDegrees)
        }
    }

    private object MeeusMoon {
        // D, M, M', F, Σl (1e-6°), Σr (1e-3 km) — Meeus Table 47.A, largest terms.
        private val longitudeTerms =
            arrayOf(
                intArrayOf(0, 0, 1, 0, 6288774, -20905355),
                intArrayOf(2, 0, -1, 0, 1274027, -3699111),
                intArrayOf(2, 0, 0, 0, 658314, -2955968),
                intArrayOf(0, 0, 2, 0, 213618, -569925),
                intArrayOf(0, 1, 0, 0, -185116, 48888),
                intArrayOf(0, 0, 0, 2, -114332, -3149),
                intArrayOf(2, 0, -2, 0, 58793, 246158),
                intArrayOf(2, -1, -1, 0, 57066, -152138),
                intArrayOf(2, 0, 1, 0, 53322, -170733),
                intArrayOf(2, -1, 0, 0, 45758, -204586),
                intArrayOf(0, 1, -1, 0, -40923, -129620),
                intArrayOf(1, 0, 0, 0, -34720, 108743),
                intArrayOf(0, 1, 1, 0, -30383, 104755),
                intArrayOf(2, 0, 0, -2, 15327, 10321),
                intArrayOf(0, 0, 1, 2, -12528, 0),
                intArrayOf(0, 0, 1, -2, 10980, 79661),
                intArrayOf(4, 0, -1, 0, 10675, -34782),
                intArrayOf(0, 0, 3, 0, 10034, -23210),
                intArrayOf(4, 0, -2, 0, 8548, -21636),
                intArrayOf(2, 1, -1, 0, -7888, 24208),
                intArrayOf(2, 1, 0, 0, -6766, 30824),
                intArrayOf(1, 0, -1, 0, -5163, -8379),
                intArrayOf(1, 1, 0, 0, 4987, -16675),
                intArrayOf(2, -1, 1, 0, 4036, -12831),
                intArrayOf(2, 0, 2, 0, 3994, -10445),
                intArrayOf(4, 0, 0, 0, 3861, -11650),
                intArrayOf(2, 0, -3, 0, 3665, 14403),
                intArrayOf(0, 1, -2, 0, -2689, -7003),
                intArrayOf(2, 0, -1, 2, -2602, 0),
                intArrayOf(2, -1, -2, 0, 2390, 10056),
                intArrayOf(1, 0, 1, 0, -2348, 6322),
                intArrayOf(2, -2, 0, 0, 2236, -9884),
            )

        // D, M, M', F, Σb (1e-6°) — Meeus Table 47.B, largest terms.
        private val latitudeTerms =
            arrayOf(
                intArrayOf(0, 0, 0, 1, 5128122),
                intArrayOf(0, 0, 1, 1, 280602),
                intArrayOf(0, 0, 1, -1, 277693),
                intArrayOf(2, 0, 0, -1, 173237),
                intArrayOf(2, 0, -1, 1, 55413),
                intArrayOf(2, 0, -1, -1, 46271),
                intArrayOf(2, 0, 0, 1, 32573),
                intArrayOf(0, 0, 2, 1, 17198),
                intArrayOf(2, 0, 1, -1, 9266),
                intArrayOf(0, 0, 2, -1, 8822),
                intArrayOf(2, -1, 0, -1, 8216),
                intArrayOf(2, 0, -2, -1, 4324),
                intArrayOf(2, 0, 1, 1, 4200),
                intArrayOf(2, 1, 0, -1, -3359),
                intArrayOf(2, -1, -1, 1, 2463),
                intArrayOf(2, -1, 0, 1, 2211),
                intArrayOf(2, -1, -1, -1, 2065),
                intArrayOf(0, 1, -1, -1, -1870),
                intArrayOf(4, 0, -1, -1, 1828),
                intArrayOf(0, 1, 0, 1, -1794),
            )

        fun at(
            t: Double,
            nutation: Nutation,
        ): Equatorial {
            val lp = 218.3164477 + 481267.88123421 * t
            val args =
                doubleArrayOf(
                    297.8501921 + 445267.1114034 * t,
                    357.5291092 + 35999.0502909 * t,
                    134.9633964 + 477198.8675055 * t,
                    93.2720950 + 483202.0175233 * t,
                )
            val e = 1 - 0.002516 * t - 0.0000074 * t * t
            var sumL = 0.0
            var sumR = 0.0
            for (term in longitudeTerms) {
                val (angle, factor) = argument(term, args, e)
                sumL += factor * term[4] * sin(angle)
                sumR += factor * term[5] * cos(angle)
            }
            var sumB = 0.0
            for (term in latitudeTerms) {
                val (angle, factor) = argument(term, args, e)
                sumB += factor * term[4] * sin(angle)
            }
            val a1 = rad(119.75 + 131.849 * t)
            val a2 = rad(53.09 + 479264.290 * t)
            val a3 = rad(313.45 + 481266.484 * t)
            val lpr = rad(lp)
            val f = rad(args[3])
            val mp = rad(args[2])
            sumL += 3958 * sin(a1) + 1962 * sin(lpr - f) + 318 * sin(a2)
            sumB += -2235 * sin(lpr) + 382 * sin(a3) + 175 * sin(a1 - f) + 175 * sin(a1 + f) +
                127 * sin(lpr - mp) - 115 * sin(lpr + mp)
            val lambda = lp + sumL / 1e6 + nutation.longitudeDegrees
            return toEquatorial(lambda, sumB / 1e6, 385000.56 + sumR / 1000, nutation.obliquityDegrees)
        }

        private fun argument(
            term: IntArray,
            args: DoubleArray,
            e: Double,
        ): Pair<Double, Double> {
            val angle = rad(term[0] * args[0] + term[1] * args[1] + term[2] * args[2] + term[3] * args[3])
            val factor =
                when (kotlin.math.abs(term[1])) {
                    1 -> e
                    2 -> e * e
                    else -> 1.0
                }
            return angle to factor
        }
    }

    private fun toEquatorial(
        lambdaDegrees: Double,
        betaDegrees: Double,
        distanceKm: Double,
        obliquityDegrees: Double,
    ): Equatorial {
        val l = rad(lambdaDegrees)
        val b = rad(betaDegrees)
        val eps = rad(obliquityDegrees)
        val ra = atan2(sin(l) * cos(eps) - tan(b) * sin(eps), cos(l))
        val dec = asin(sin(b) * cos(eps) + cos(b) * sin(eps) * sin(l))
        return Equatorial(deg(ra), deg(dec), distanceKm)
    }

    /** Apparent local sidereal time (Meeus eq. 12.4 plus the equation of the equinoxes). */
    private fun localSiderealDegrees(
        jdUt: Double,
        nutation: Nutation,
        eastLongitude: Double,
    ): Double {
        val t = (jdUt - JD_J2000) / DAYS_PER_CENTURY
        val mean =
            280.46061837 + 360.98564736629 * (jdUt - JD_J2000) + 0.000387933 * t * t - t * t * t / 38710000
        return mean + nutation.longitudeDegrees * cos(rad(nutation.obliquityDegrees)) + eastLongitude
    }

    /** Meeus ch. 40: topocentric right ascension and declination for horizontal parallax sin π = [sinParallax]. */
    private fun topocentric(
        body: Equatorial,
        place: Coordinates,
        lstDegrees: Double,
        sinParallax: Double,
    ): Topocentric {
        val phi = rad(place.latitude)
        val u = atan(0.99664719 * tan(phi))
        val heightRatio = place.elevationMeters / 6_378_140.0
        val rhoSin = 0.99664719 * sin(u) + heightRatio * sin(phi)
        val rhoCos = cos(u) + heightRatio * cos(phi)
        val h = rad(lstDegrees - body.raDegrees)
        val dec = rad(body.decDegrees)
        val denominator = cos(dec) - rhoCos * sinParallax * cos(h)
        val dRa = atan2(-rhoCos * sinParallax * sin(h), denominator)
        val decTopo = atan2((sin(dec) - rhoSin * sinParallax) * cos(dRa), denominator)
        // Ratio of topocentric to geocentric distance, from the rectangular components.
        val x = cos(dec) * cos(h) - rhoCos * sinParallax
        val y = cos(dec) * sin(h)
        val z = sin(dec) - rhoSin * sinParallax
        return Topocentric(body.raDegrees + deg(dRa), deg(decTopo), deg(h - dRa), sqrt(x * x + y * y + z * z))
    }

    private fun altitude(
        body: Topocentric,
        place: Coordinates,
    ): Double {
        val phi = rad(place.latitude)
        val dec = rad(body.decDegrees)
        return deg(asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(rad(body.hourAngleDegrees))))
    }

    private fun separation(
        a: Topocentric,
        b: Topocentric,
    ): Double {
        val d1 = rad(a.decDegrees)
        val d2 = rad(b.decDegrees)
        val cosine = sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(rad(a.raDegrees - b.raDegrees))
        return deg(acos(cosine.coerceIn(-1.0, 1.0)))
    }

    /** ΔT in seconds from the Morrison–Stephenson parabola −20 + 32 u², u in centuries from 1820 (ample here). */
    private fun deltaTSeconds(jdUt: Double): Double {
        val u = (jdUt - JD_J2000) / DAYS_PER_CENTURY + 1.8
        return -20 + 32 * u * u
    }

    private fun rad(degrees: Double): Double = degrees * PI / 180

    private fun deg(radians: Double): Double = radians * 180 / PI
}
