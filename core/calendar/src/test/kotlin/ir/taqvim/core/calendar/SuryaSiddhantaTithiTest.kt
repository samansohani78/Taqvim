/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Test

/** DT-043: the tithi the app displays is the one its own Nepali festivals are computed with (ADR-0038). */
@OptIn(ExperimentalKotest::class)
class SuryaSiddhantaTithiTest {
    private val kathmandu = TimeZone.of("Asia/Kathmandu")
    private val propertyConfig = PropTestConfig(seed = 20_261_006L, iterations = PropertyTesting.iterations)

    @Test
    fun `the displayed tithi is the one the festival rule read at that festival's own moment`() {
        // The festival rule and the display must never disagree, because they are the same engine asked the same
        // question. Each case is a day the rule picks for a festival of a known tithi; reading the tithi at sunrise
        // in Kathmandu on that day must give that tithi back.
        val cases =
            listOf(
                // Dashain's Vijaya Dashami, BS 2082: lunar month 6, tithi 10 (shukla).
                Triple(2082, 6, 10),
                // Laxmi Puja's Amavasya, BS 2082: lunar month 7, tithi 30.
                Triple(2082, 7, 30),
                // Janai Purnima, BS 2083: lunar month 4, tithi 15.
                Triple(2083, 4, 15),
            )
        cases.forEach { (year, month, tithi) ->
            val day = NepaliLunarDays.days(year, month, tithi).first()
            val sunrise = KathmanduDaylight.sunrise(day.value)
            val instant = instantOf(day, sunrise)

            SuryaSiddhantaTithi.at(instant).number shouldBe tithi
        }
    }

    @Test
    fun `tithi 15 is the full moon and 30 the new moon`() {
        SuryaSiddhantaTithi.ofElongation(0.0) shouldBe TithiPosition(1, Paksha.SHUKLA)
        SuryaSiddhantaTithi.ofElongation(179.0) shouldBe TithiPosition(15, Paksha.SHUKLA)
        SuryaSiddhantaTithi.ofElongation(180.0) shouldBe TithiPosition(16, Paksha.KRISHNA)
        SuryaSiddhantaTithi.ofElongation(359.9) shouldBe TithiPosition(30, Paksha.KRISHNA)
        // Out of range in both directions wraps rather than throwing.
        SuryaSiddhantaTithi.ofElongation(-1.0) shouldBe TithiPosition(30, Paksha.KRISHNA)
        SuryaSiddhantaTithi.ofElongation(720.0) shouldBe TithiPosition(1, Paksha.SHUKLA)
    }

    @Test
    fun `every instant has a tithi in 1 to 30 with the matching paksha`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(-200_000L..200_000L)) { hours ->
                val tithi = SuryaSiddhantaTithi.at(Instant.fromEpochSeconds(hours * SECONDS_PER_HOUR))

                (tithi.number in 1..TITHIS_PER_MONTH) shouldBe true
                (tithi.numberInPaksha in 1..TITHIS_PER_PAKSHA) shouldBe true
                tithi.paksha shouldBe if (tithi.number <= TITHIS_PER_PAKSHA) Paksha.SHUKLA else Paksha.KRISHNA
            }
        }

    /** The instant at [fraction] of the Nepali civil day [day]. */
    private fun instantOf(
        day: Jdn,
        fraction: Double,
    ): Instant {
        val midnight = day.toLocalDate().atStartOfDayIn(kathmandu)
        return midnight + (fraction * SECONDS_PER_DAY).toLong().toDuration(DurationUnit.SECONDS)
    }

    private companion object {
        const val SECONDS_PER_HOUR = 3_600L
        const val SECONDS_PER_DAY = 86_400.0
        const val TITHIS_PER_MONTH = 30
        const val TITHIS_PER_PAKSHA = 15
    }
}
