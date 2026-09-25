/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of golden/usno/twilight-2026.csv. */
private const val DATE = 0
private const val CITY = 1
private const val LATITUDE = 2
private const val LONGITUDE = 3
private const val OFFSET_HOURS = 4
private const val CIVIL_DAWN = 5
private const val SUNRISE = 6
private const val TRANSIT = 7
private const val SUNSET = 8
private const val CIVIL_DUSK = 9

private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_DAY = 86_400L
private const val HALF_HOUR_SECONDS = 1_800L

/**
 * How far the blue hour's lower edge may sit from USNO civil twilight. The USNO publishes whole minutes and the
 * interval search resolves to 10 seconds, so a one-minute difference is rounding; measured across these 60 days the
 * gap is 0 or −1 minutes, never more (ADR-0047).
 */
private const val CIVIL_TWILIGHT_TOLERANCE_MINUTES = 1L

/**
 * T-407/T-401 (DT-011, DT-017): the Sun's rise, set and transit against the U.S. Naval Observatory for Tehran and
 * the four cities DT-011 names, on the 21st of each month of 2026, and the measured gap between the app's blue hour
 * and USNO's civil twilight.
 *
 * USNO defines civil twilight at a **geometric** altitude of −6° and rise/set at −0.8333° (refraction and
 * semidiameter included), and rounds to the minute. Rise, set and transit therefore compare directly, within the
 * ±3 min of the T-407 acceptance criterion.
 *
 * The blue hour's lower edge is that same civil twilight, so it compares directly too. It did not always: while
 * [PhotographyPanel.BLUE_HOUR_BOTTOM] was read as an **apparent** altitude, `Refraction.Normal` clamped its
 * correction below the horizon and the app's −6° was about −6.6° geometric, opening the blue hour 2 to 7 minutes
 * early and closing it as late — largest at Berlin, and never the other way in 120 comparisons. ADR-0047 made that
 * edge geometric, which is how the tables define it, and the gap fell to 0 or −1 minutes. DT-017 records that the
 * −4° and +6° edges of the golden hour have no published definition to check at all, so they stay apparent.
 */
class UsnoTwilightTest {
    private data class Reference(
        val date: LocalDate,
        val city: String,
        val place: Coordinates,
        val offsetSeconds: Long,
        val phenomena: Map<String, Long>,
    )

    private val tolerance = 3L * SECONDS_PER_MINUTE

    private val references: List<Reference> =
        GoldenFile
            .load("golden/usno/twilight-2026.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row ->
                Reference(
                    date = LocalDate.parse(row[DATE]),
                    city = row[CITY],
                    place = Coordinates(row[LATITUDE].toDouble(), row[LONGITUDE].toDouble()),
                    offsetSeconds = (row[OFFSET_HOURS].toDouble() * MINUTES_PER_HOUR * SECONDS_PER_MINUTE).toLong(),
                    phenomena =
                        mapOf(
                            "civil_dawn" to minutesOfDay(row[CIVIL_DAWN]),
                            "sunrise" to minutesOfDay(row[SUNRISE]),
                            "transit" to minutesOfDay(row[TRANSIT]),
                            "sunset" to minutesOfDay(row[SUNSET]),
                            "civil_dusk" to minutesOfDay(row[CIVIL_DUSK]),
                        ),
                )
            }

    private fun minutesOfDay(text: String): Long =
        text.substring(0, 2).toLong() * MINUTES_PER_HOUR + text.substring(3, 5).toLong()

    /** The instant of local standard midnight starting [reference]'s day. */
    private fun midnight(reference: Reference): Instant =
        Instant.fromEpochSeconds(reference.date.toEpochDay() * SECONDS_PER_DAY) - reference.offsetSeconds.seconds

    /** Local standard time of [instant], in minutes after [reference]'s midnight. */
    private fun localMinutes(
        reference: Reference,
        instant: Instant,
    ): Long = (instant - midnight(reference)).inWholeSeconds / SECONDS_PER_MINUTE

    private fun check(
        reference: Reference,
        name: String,
        instant: Instant?,
    ) {
        withClue("${reference.city} ${reference.date} $name") {
            instant shouldNotBe null
            val published = reference.phenomena.getValue(name)
            val computed = localMinutes(reference, requireNotNull(instant))
            abs(computed - published) * SECONDS_PER_MINUTE shouldBeLessThanOrEqual tolerance
        }
    }

    @Test
    fun `the golden covers every city and month`() {
        references shouldHaveSize 60
        references.map { it.city }.distinct() shouldHaveSize 5
    }

    @Test
    fun `sunrise, sunset and transit match the USNO within three minutes`() {
        references.forEach { reference ->
            val events = Sky.riseSetTransit(CelestialBody.SUN, reference.place, midnight(reference))
            check(reference, "sunrise", events.rise)
            check(reference, "sunset", events.set)
            check(reference, "transit", events.transit)
        }
    }

    @Test
    fun `the blue hour begins and ends at the USNO civil twilight`() {
        references.forEach { reference ->
            val day = PhotographyPanel.day(reference.place, midnight(reference))
            withClue("${reference.city} ${reference.date} blue hours ${day.blueHours}") {
                day.blueHours shouldHaveSize 2
            }
            val (morning, evening) = day.blueHours
            val dawnGap = localMinutes(reference, morning.start) - reference.phenomena.getValue("civil_dawn")
            val duskGap = localMinutes(reference, evening.end) - reference.phenomena.getValue("civil_dusk")
            withClue("${reference.city} ${reference.date} dawn $dawnGap min, dusk $duskGap min") {
                abs(dawnGap) shouldBeLessThanOrEqual CIVIL_TWILIGHT_TOLERANCE_MINUTES
                abs(duskGap) shouldBeLessThanOrEqual CIVIL_TWILIGHT_TOLERANCE_MINUTES
            }
        }
    }

    @Test
    fun `the blue hour's lower edge is the geometric altitude the tables define`() {
        val berlin = references.first { it.city == "Berlin" }
        val day = PhotographyPanel.day(berlin.place, midnight(berlin))
        val dusk = day.blueHours[1].end
        val altitudes = Sky.altitudes(CelestialBody.SUN, dusk, berlin.place)
        withClue("Berlin ${berlin.date} at the blue hour's end: $altitudes") {
            // Geometric is what civil twilight is defined at; apparent is about 0.57° higher, which is the whole
            // reason this edge moved (ADR-0047). Asserting both pins the convention, not just the resulting time.
            altitudes.geometricDegrees shouldBe (PhotographyPanel.BLUE_HOUR_BOTTOM plusOrMinus 0.05)
            altitudes.apparentDegrees shouldBeGreaterThan PhotographyPanel.BLUE_HOUR_BOTTOM + 0.3
        }
    }

    @Test
    fun `the sun is highest at the published transit`() {
        references.forEach { reference ->
            val transit = midnight(reference) + (reference.phenomena.getValue("transit") * SECONDS_PER_MINUTE).seconds
            val altitude = { offset: Long ->
                Sky.skyPosition(CelestialBody.SUN, transit + offset.seconds, reference.place).altitudeDegrees
            }
            withClue("${reference.city} ${reference.date}") {
                val noon = altitude(0)
                noon shouldBeGreaterThan altitude(-HALF_HOUR_SECONDS)
                noon shouldBeGreaterThan altitude(HALF_HOUR_SECONDS)
            }
        }
    }
}
