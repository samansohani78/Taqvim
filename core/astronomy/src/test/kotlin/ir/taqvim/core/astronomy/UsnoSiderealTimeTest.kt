/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldNotBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of golden/usno/sidereal-2026.csv. */
private const val DATE = 0
private const val UT1 = 1
private const val PLACE = 2
private const val LATITUDE = 3
private const val LONGITUDE = 4
private const val GAST = 5
private const val LAST = 6

private const val SECONDS_PER_DAY = 86_400L
private const val DEGREES_PER_HOUR = 15.0
private const val HOURS_PER_DAY = 24.0
private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0

/**
 * Largest accepted difference in seconds of time. UT1 and UTC differ by up to 0.9 s and the golden is requested at
 * UT1, so anything below a second cannot be attributed to the app; the measured worst case is far smaller.
 */
private const val TOLERANCE_SECONDS = 1.0

/** Largest accepted difference for the midheaven, in degrees — a second of time is about 0.0042°. */
private const val MIDHEAVEN_TOLERANCE_DEGREES = 0.01

/**
 * A-14 (DT-018): the local apparent sidereal time every chart angle is built on, against the U.S. Naval Observatory,
 * for eight places spanning both hemispheres, the prime meridian and both sides of the date line, at 00:00 and 12:00
 * UT1 on the 2026 equinoxes and solstices.
 *
 * [Houses.ramcDegrees] is local apparent sidereal time expressed as an angle, and it is the single input to the
 * midheaven, the ascendant and all twelve Placidus cusps — so an error here would move a whole chart, while the cusp
 * construction itself still has no published chart to check against (DT-018 stays open for that).
 *
 * The golden is requested at UT1 and the app is given UTC. They differ by at most 0.9 s, which is 0.0037° of right
 * ascension, so the tolerances below are set by that rather than by the ephemeris. The app is far closer than it has
 * to be: measured over these 64 rows the worst difference is **0.010 s** of sidereal time and **4.5e-5°** of
 * midheaven, so a tolerance this wide will still catch any real drift.
 */
class UsnoSiderealTimeTest {
    private data class Reference(
        val date: LocalDate,
        val ut1: String,
        val place: String,
        val coordinates: Coordinates,
        val greenwichHours: Double,
        val localHours: Double,
    )

    private val references: List<Reference> =
        GoldenFile
            .load("golden/usno/sidereal-2026.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row ->
                Reference(
                    date = LocalDate.parse(row[DATE]),
                    ut1 = row[UT1],
                    place = row[PLACE],
                    coordinates = Coordinates(row[LATITUDE].toDouble(), row[LONGITUDE].toDouble()),
                    greenwichHours = hours(row[GAST]),
                    localHours = hours(row[LAST]),
                )
            }

    /** A USNO `h:mm:ss.ssss` sidereal time as decimal hours. */
    private fun hours(text: String): Double {
        val parts = text.split(':')
        return parts[0].toInt() + parts[1].toInt() / 60.0 + parts[2].toDouble() / 3600.0
    }

    private fun instant(reference: Reference): Instant {
        val seconds = reference.ut1.split(':').let { it[0].toLong() * 3600 + it[1].toLong() * 60 + it[2].toLong() }
        return Instant.fromEpochSeconds(reference.date.toEpochDay() * SECONDS_PER_DAY + seconds)
    }

    /** The smallest difference between two angles in degrees, allowing for the wrap at 360°. */
    private fun separation(
        first: Double,
        second: Double,
    ): Double = abs((first - second + HALF_TURN).mod(FULL_TURN) - HALF_TURN)

    @Test
    fun `the golden covers every place, date and hour`() {
        references shouldHaveSize 64
        references.map { it.place }.distinct() shouldHaveSize 8
        references.map { it.date }.distinct() shouldHaveSize 4
    }

    @Test
    fun `local apparent sidereal time matches the USNO`() {
        references.forEach { reference ->
            val computed = Houses.ramcDegrees(instant(reference), reference.coordinates)
            val published = reference.localHours * DEGREES_PER_HOUR
            val seconds = separation(computed, published) / DEGREES_PER_HOUR * 3600
            withClue("${reference.place} ${reference.date} ${reference.ut1}: $seconds s") {
                seconds shouldBeLessThan TOLERANCE_SECONDS
            }
        }
    }

    @Test
    fun `Greenwich apparent sidereal time matches the USNO`() {
        references.filter { it.place == "Greenwich" }.forEach { reference ->
            // Greenwich is 0.0° east in the golden, so the local angle is the Greenwich one and the two columns
            // check the same number from opposite ends: the app's engine against USNO's gast.
            val computed = Houses.ramcDegrees(instant(reference), Coordinates(reference.coordinates.latitude, 0.0))
            val seconds =
                separation(computed, reference.greenwichHours * DEGREES_PER_HOUR) / DEGREES_PER_HOUR * 3600
            withClue("Greenwich ${reference.date} ${reference.ut1}: $seconds s") {
                seconds shouldBeLessThan TOLERANCE_SECONDS
            }
        }
    }

    @Test
    fun `the midheaven is the ecliptic longitude of the published sidereal time`() {
        references.filter { abs(it.coordinates.latitude) < 60 }.forEach { reference ->
            val moment = instant(reference)
            val cusps = Houses.placidus(moment, reference.coordinates)
            withClue("${reference.place} ${reference.date} ${reference.ut1}") { cusps shouldNotBe null }
            val obliquity = Math.toRadians(Houses.obliquityDegrees(moment))
            // Derived here from USNO's own number, with plain trigonometry rather than the app's helper: the
            // midheaven is where the meridian crosses the ecliptic, so tan(longitude) = tan(ramc) / cos(obliquity).
            val ramc = Math.toRadians(reference.localHours * DEGREES_PER_HOUR)
            val expected = Math.toDegrees(atan2(sin(ramc), cos(ramc) * cos(obliquity))).mod(FULL_TURN)
            val gap = separation(requireNotNull(cusps).midheaven, expected)
            withClue("${reference.place} ${reference.date} ${reference.ut1}: $gap°") {
                gap shouldBeLessThan MIDHEAVEN_TOLERANCE_DEGREES
            }
        }
    }

    @Test
    fun `the sidereal day is shorter than the solar day`() {
        // A sanity check on the golden itself: between 00:00 and 12:00 UT1 the sidereal clock advances 12 hours plus
        // about 2 minutes, which is what makes the sidereal day about 4 minutes short of the solar one.
        references
            .groupBy { it.place to it.date }
            .forEach { (key, pair) ->
                val (midnight, noon) = pair.sortedBy { it.ut1 }
                val advance = (noon.localHours - midnight.localHours).mod(HOURS_PER_DAY)
                withClue("$key advance $advance h") {
                    abs(advance - (HOURS_PER_DAY / 2 + 2.0 / 60)) shouldBeLessThan 0.01
                }
            }
    }
}
