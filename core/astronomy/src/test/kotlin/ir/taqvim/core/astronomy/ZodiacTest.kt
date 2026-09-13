/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/**
 * T-404: sign boundaries, IAU constellation lookups and the "Moon in Scorpio" search. No published list of
 * Moon-in-Scorpio dates is available yet (DATA_TODO), so the search is checked for internal consistency.
 */
class ZodiacTest {
    private val year2026 = Instant.parse("2026-01-01T00:00:00Z") to Instant.parse("2027-01-01T00:00:00Z")

    @Test
    fun `tropical sign boundaries`() {
        ZodiacSign.ofEclipticLongitude(0.0) shouldBe ZodiacSign.ARIES
        ZodiacSign.ofEclipticLongitude(29.9999) shouldBe ZodiacSign.ARIES
        ZodiacSign.ofEclipticLongitude(30.0) shouldBe ZodiacSign.TAURUS
        ZodiacSign.ofEclipticLongitude(210.0) shouldBe ZodiacSign.SCORPIO
        ZodiacSign.ofEclipticLongitude(239.9999) shouldBe ZodiacSign.SCORPIO
        ZodiacSign.ofEclipticLongitude(359.9999) shouldBe ZodiacSign.PISCES
        ZodiacSign.ofEclipticLongitude(-0.0001) shouldBe ZodiacSign.PISCES
        ZodiacSign.ofEclipticLongitude(720.0) shouldBe ZodiacSign.ARIES
    }

    @Test
    fun `bright stars far from constellation boundaries`() {
        // Rounded J2000 positions used only as test inputs; each star lies well inside its constellation.
        Zodiac.iauConstellation(16.490, -26.43) shouldBe "Sco" // Antares
        Zodiac.iauConstellation(10.140, 11.97) shouldBe "Leo" // Regulus
        Zodiac.iauConstellation(13.420, -11.16) shouldBe "Vir" // Spica
        Zodiac.iauConstellation(4.599, 16.51) shouldBe "Tau" // Aldebaran
        Zodiac.iauConstellation(2.530, 89.26) shouldBe "UMi" // Polaris
    }

    @Test
    fun `the Moon visits Scorpius about once a sidereal month`() {
        val intervals = Zodiac.moonInScorpio(year2026.first, year2026.second)

        intervals.size shouldBeInRange 12..14
        intervals.forEach { (start, end) ->
            val middle = start + (end - start) / 2
            Zodiac.moonConstellation(middle) shouldBe Zodiac.SCORPIUS
            if (start >
                year2026.first
            ) {
                Zodiac.moonConstellation(start - 2.minutes) shouldBe notScorpius(start - 2.minutes)
            }
            if (end < year2026.second) Zodiac.moonConstellation(end + 1.minutes) shouldBe notScorpius(end + 1.minutes)
            (end - start).inWholeHours.toInt() shouldBeInRange 1..(4 * 24)
        }
        intervals.zipWithNext().forEach { (earlier, later) -> ((later.start - earlier.start) > 20.days) shouldBe true }
    }

    @Test
    fun `tropical Scorpio lasts about two and a half days`() {
        val intervals = Zodiac.moonInScorpio(year2026.first, year2026.second, ZodiacSystem.TROPICAL)

        intervals.size shouldBeInRange 13..14
        intervals.drop(1).dropLast(1).forEach { (start, end) ->
            Zodiac.tropicalSign(CelestialBody.MOON, start + (end - start) / 2) shouldBe ZodiacSign.SCORPIO
            (end - start).inWholeHours.toInt() shouldBeInRange 48..66
        }
    }

    @Test
    fun `windows are validated and clipped`() {
        shouldThrow<IllegalArgumentException> { Zodiac.moonInScorpio(year2026.second, year2026.first) }
        val scorpio = Zodiac.moonInScorpio(year2026.first, year2026.second, ZodiacSystem.TROPICAL)[3]
        val inside = scorpio.start + 12.hours
        Zodiac.moonInScorpio(inside, inside + 1.hours, ZodiacSystem.TROPICAL) shouldBe
            listOf(TimeInterval(inside, inside + 1.hours))
        Zodiac.moonInScorpio(scorpio.end + 1.days, scorpio.end + 2.days, ZodiacSystem.TROPICAL).shouldBeEmpty()
        Zodiac.tropicalSign(CelestialBody.SUN, Instant.parse("2026-11-01T00:00:00Z")) shouldBe ZodiacSign.SCORPIO
    }

    /** The constellation actually reported at [instant], asserted not to be Scorpius. */
    private fun notScorpius(instant: Instant): String =
        Zodiac.moonConstellation(instant).also { (it != Zodiac.SCORPIUS) shouldBe true }
}
