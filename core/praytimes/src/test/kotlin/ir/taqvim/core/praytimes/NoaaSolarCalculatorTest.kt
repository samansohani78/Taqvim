/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** A-09 against NOAA's own spreadsheet results (golden/noaa) plus physical invariants. */
class NoaaSolarCalculatorTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_921L, iterations = PropertyTesting.iterations)

    private val noaa = NoaaSolarCalculator

    private val rows: List<Map<String, Double>> =
        GoldenFile.load("golden/noaa/noaa-solar-day-2010-06-21.csv").lines.let { lines ->
            val header = lines.first().split(',')
            lines.drop(1).map { line -> header.zip(line.split(',').map(String::toDouble)).toMap() }
        }

    private fun Map<String, Double>.instant(): Instant =
        Instant.fromEpochMilliseconds(
            LOCAL_MIDNIGHT_UTC.toEpochMilliseconds() + (getValue("time_fraction") * MILLIS_PER_DAY).roundToLong(),
        )

    @Test
    fun `all 240 spreadsheet rows are reproduced`() {
        rows shouldHaveSize 240
        rows.forEach { row ->
            val instant = row.instant()
            val julianDay = noaa.julianDay(instant)
            val sun = noaa.parameters(julianDay)
            val day = noaa.solarDay(julianDay, LATITUDE, LONGITUDE, UTC_OFFSET_HOURS)
            val position = noaa.horizontal(instant, LATITUDE, LONGITUDE)
            withClue("row at ${row.getValue("time_fraction")}") {
                julianDay shouldBe (row.getValue("julian_day") plusOrMinus 1e-7)
                sun.declinationDegrees shouldBe (row.getValue("declination_deg") plusOrMinus TOLERANCE)
                sun.equationOfTimeMinutes shouldBe (row.getValue("equation_of_time_min") plusOrMinus TOLERANCE)
                day.solarNoonMinutes shouldBe
                    (row.getValue("solar_noon_lst_day") * MINUTES_PER_DAY plusOrMinus TOLERANCE)
                day.sunriseMinutes.shouldNotBeNull() shouldBe
                    (row.getValue("sunrise_lst_day") * MINUTES_PER_DAY plusOrMinus TOLERANCE)
                day.sunsetMinutes.shouldNotBeNull() shouldBe
                    (row.getValue("sunset_lst_day") * MINUTES_PER_DAY plusOrMinus TOLERANCE)
                position.zenithDegrees shouldBe (row.getValue("zenith_deg") plusOrMinus TOLERANCE)
                position.refractionDegrees shouldBe (row.getValue("refraction_deg") plusOrMinus TOLERANCE)
                position.apparentElevationDegrees shouldBe
                    (row.getValue("elevation_corrected_deg") plusOrMinus TOLERANCE)
                position.azimuthDegrees shouldBe (row.getValue("azimuth_deg") plusOrMinus TOLERANCE)
            }
        }
    }

    @Test
    fun `polar days and nights have no sunrise or sunset`() {
        noaa.hourAngleDegrees(latitudeDegrees = 80.0, declinationDegrees = 23.4).shouldBeNull()
        noaa.hourAngleDegrees(latitudeDegrees = -80.0, declinationDegrees = 23.4).shouldBeNull()
        noaa.hourAngleDegrees(latitudeDegrees = 0.0, declinationDegrees = 0.0).shouldNotBeNull() shouldBe
            (90.83 plusOrMinus 0.01)
        val polarNight = noaa.solarDay(noaa.julianDay(Instant.parse("2026-12-21T12:00:00Z")), 78.2, 15.6, 1.0)
        polarNight.sunriseMinutes.shouldBeNull()
        polarNight.sunsetMinutes.shouldBeNull()
    }

    @Test
    fun `positions stay finite even with the Sun overhead or at a pole`() {
        val instant = Instant.parse("2026-06-21T08:30:00Z")
        val declination = noaa.parameters(noaa.julianDay(instant)).declinationDegrees
        listOf(declination to 52.5, 90.0 to 0.0, -90.0 to 0.0).forEach { (latitude, longitude) ->
            val position = noaa.horizontal(instant, latitude, longitude)
            listOf(position.zenithDegrees, position.azimuthDegrees, position.refractionDegrees).forEach {
                it.isFinite() shouldBe true
            }
        }
    }

    @Test
    fun `declination and equation of time change continuously`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(FIRST_VALID_MILLIS..LAST_VALID_MILLIS)) { millis ->
                val now = noaa.parameters(noaa.julianDay(Instant.fromEpochMilliseconds(millis)))
                val minuteLater =
                    noaa.parameters(
                        noaa.julianDay(
                            Instant.fromEpochMilliseconds(millis + MILLIS_PER_MINUTE),
                        ),
                    )
                abs(minuteLater.declinationDegrees - now.declinationDegrees) shouldBeLessThan 0.001
                abs(minuteLater.equationOfTimeMinutes - now.equationOfTimeMinutes) shouldBeLessThan 0.001
                // Obliquity plus nutation reaches about 23.4505° early in the 20th century.
                abs(now.declinationDegrees) shouldBeLessThan 23.46
            }
        }

    @Test
    fun `azimuth grows through the morning at mid-latitudes`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(-60..60)) { wholeDegrees ->
                val longitude = wholeDegrees.toDouble()
                val noonUtcMinutes = 720 - 4 * longitude
                val morning =
                    Instant.fromEpochMilliseconds(
                        DAY_START_MILLIS + ((noonUtcMinutes - 120) * MILLIS_PER_MINUTE).roundToLong(),
                    )
                val later = Instant.fromEpochMilliseconds(morning.toEpochMilliseconds() + 30 * MILLIS_PER_MINUTE)
                noaa.horizontal(morning, 35.0, longitude).azimuthDegrees shouldBeLessThan
                    noaa.horizontal(later, 35.0, longitude).azimuthDegrees
            }
        }

    private companion object {
        const val LATITUDE = 40.0
        const val LONGITUDE = -105.0
        const val UTC_OFFSET_HOURS = -7.0
        const val TOLERANCE = 1e-6
        const val MINUTES_PER_DAY = 1_440.0
        const val MILLIS_PER_DAY = 86_400_000.0
        const val MILLIS_PER_MINUTE = 60_000L
        val LOCAL_MIDNIGHT_UTC: Instant = Instant.parse("2010-06-21T07:00:00Z")
        val FIRST_VALID_MILLIS: Long = Instant.parse("1901-01-01T00:00:00Z").toEpochMilliseconds()
        val LAST_VALID_MILLIS: Long = Instant.parse("2099-12-30T00:00:00Z").toEpochMilliseconds()
        val DAY_START_MILLIS: Long = Instant.parse("2026-04-15T00:00:00Z").toEpochMilliseconds()
    }
}
