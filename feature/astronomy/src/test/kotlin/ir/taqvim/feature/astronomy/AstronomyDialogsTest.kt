/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.astronomy.ClassicalPlanet
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.ZodiacSystem
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-1300 dialogs: horoscope and year horoscope, planetary hours and the Moon in Scorpio. */
class AstronomyDialogsTest {
    private val tehran = AstronomyFixtures.tehran()
    private val sundayNoon = AstronomyFixtures.at("2026-06-21T12:00", tehran)

    @Test
    fun `a horoscope has twelve cusps, the angles and the lots`() {
        val dialog =
            AstronomyDialogs
                .build(AstronomyDialogKind.HOROSCOPE, tehran, sundayNoon)
                .shouldBeInstanceOf<AstronomyDialog.Horoscope>()
        val chart = dialog.chart.shouldNotBeNull()

        dialog.yearly shouldBe false
        dialog.at shouldBe AstronomyText(tehran).dateTime(sundayNoon)
        chart.cusps.size shouldBe 12
        chart.cusps.first().house shouldBe "1"
        chart.cusps.first().position shouldBe chart.ascendant
        chart.cusps[9].position shouldBe chart.midheaven
        chart.cuspLongitudes.size shouldBe 12
        chart.dayChart shouldBe true

        val polar = AstronomyFixtures.tromso()
        AstronomyDialogs
            .build(AstronomyDialogKind.HOROSCOPE, polar, sundayNoon)
            .shouldBeInstanceOf<AstronomyDialog.Horoscope>()
            .chart
            .shouldBeNull()
    }

    @Test
    fun `the year horoscope is cast for the March equinox that began the year`() {
        AstronomyDialogs.yearStart(sundayNoon) shouldBe Sky.seasons(2026).marchEquinox
        AstronomyDialogs.yearStart(Instant.parse("2026-01-10T00:00:00Z")) shouldBe Sky.seasons(2025).marchEquinox
        val dialog =
            AstronomyDialogs
                .build(AstronomyDialogKind.YEAR_HOROSCOPE, tehran, sundayNoon)
                .shouldBeInstanceOf<AstronomyDialog.Horoscope>()
        dialog.yearly shouldBe true
        dialog.at shouldBe AstronomyText(tehran).dateTime(Sky.seasons(2026).marchEquinox)
    }

    @Test
    fun `planetary hours of a Sunday begin with the Sun and mark the current hour`() {
        val dialog =
            AstronomyDialogs
                .build(AstronomyDialogKind.PLANETARY_HOURS, tehran, sundayNoon)
                .shouldBeInstanceOf<AstronomyDialog.PlanetaryHours>()

        dialog.hours.size shouldBe 24
        dialog.hours.first().ruler shouldBe ClassicalPlanet.SUN
        dialog.hours.count { it.isCurrent } shouldBe 1
        dialog.hours.take(12).all { it.daytime } shouldBe true
        dialog.hours.first().span shouldStartWith "04:"

        val polar = AstronomyFixtures.tromso()
        AstronomyDialogs
            .build(AstronomyDialogKind.PLANETARY_HOURS, polar, AstronomyFixtures.at("2026-06-21T12:00", polar))
            .shouldBeInstanceOf<AstronomyDialog.PlanetaryHours>()
            .hours
            .shouldBeEmpty()
    }

    @Test
    fun `the Moon is in tropical Scorpio thirteen or fourteen times in 1405`() {
        val tropical = AstronomyFixtures.tehran(scorpio = ZodiacSystem.TROPICAL)
        val dialog =
            AstronomyDialogs
                .build(AstronomyDialogKind.MOON_IN_SCORPIO, tropical, sundayNoon)
                .shouldBeInstanceOf<AstronomyDialog.MoonInScorpio>()

        dialog.yearTitle shouldBe "1405"
        dialog.system shouldBe ZodiacSystem.TROPICAL
        dialog.periods.size shouldBeInRange 13..14
    }
}
