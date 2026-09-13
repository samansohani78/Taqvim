/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Qibla
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.feature.compass.CompassAccuracy.MEDIUM
import org.junit.jupiter.api.Test

/** T-1302 markers, true north and localized texts. */
class CompassStateMapperTest {
    private val tehran = CompassFixtures.TEHRAN
    private val sky = CompassStateMapper.snapshot(tehran, CompassFixtures.NOON, CompassFixtures.FIVE_EAST)

    private fun dial(
        settings: CompassSettings = CompassFixtures.settings(),
        heading: Double = 358.0,
        announced: Int? = null,
        snapshot: SkySnapshot? = sky,
    ): CompassContent.Dial =
        CompassStateMapper
            .map(settings, HeadingReading.Measured(heading, MEDIUM), snapshot, announced, CompassUiState())
            .content
            .shouldBeInstanceOf<CompassContent.Dial>()

    @Test
    fun `the sky snapshot of Tehran at noon`() {
        sky.declinationDegrees shouldBe 5.0
        val qibla = Qibla.bearingDegrees(tehran.coordinates).shouldNotBeNull()
        sky.qiblaDegrees.shouldNotBeNull() shouldBe (qibla plusOrMinus 1e-9)
        sky.sun shouldBe Sky.skyPosition(CelestialBody.SUN, CompassFixtures.NOON, tehran.coordinates)
        sky.moon shouldBe Sky.skyPosition(CelestialBody.MOON, CompassFixtures.NOON, tehran.coordinates)
        (sky.sun.altitudeDegrees > 70.0) shouldBe true
        sky.sunPath.size shouldBeInRange 13..16
        sky.sunPath.forEach { (instant, position) ->
            (position.altitudeDegrees > 0.0) shouldBe true
            (instant > CompassFixtures.NOON) shouldBe true
            instant.epochSeconds % 3_600 shouldBe 0L
        }
    }

    @Test
    fun `loading and unavailable states`() {
        val flags = CompassUiState(frozen = true, showSunPath = true)
        CompassStateMapper.map(null, HeadingReading.Measured(1.0, CompassAccuracy.HIGH), sky, null, flags) shouldBe
            flags.copy(content = CompassContent.Loading)
        val noHeading = CompassStateMapper.map(CompassFixtures.settings(), null, sky, null, flags)
        noHeading.content shouldBe CompassContent.Loading
        CompassStateMapper.map(null, HeadingReading.Unavailable, null, null, flags).content shouldBe
            CompassContent.SensorUnavailable
    }

    @Test
    fun `true north applies the declination and the Qibla turn follows the heading`() {
        val dial = dial()
        dial.headingDegrees shouldBe (3f plusOrMinus 1e-4f)
        dial.headingText shouldBe "3"
        dial.cardinal shouldBe Cardinal.N
        dial.north shouldBe NorthReference.TRUE
        dial.announcedDegrees shouldBe 0
        dial.placeName shouldBe "Tehran"
        dial.accuracy shouldBe MEDIUM
        val qibla = dial.qibla.shouldNotBeNull()
        qibla.turnDegrees.toDouble() shouldBe (Angles.delta(3.0, sky.qiblaDegrees.shouldNotBeNull()) plusOrMinus 1e-4)
        (qibla.turnDegrees < 0f) shouldBe true
        qibla.aligned shouldBe false
        dial(heading = sky.qiblaDegrees.shouldNotBeNull() - 5.0).qibla.shouldNotBeNull().aligned shouldBe true
        dial.sun.shouldNotBeNull().aboveHorizon shouldBe true
        dial.sunPath.size shouldBe sky.sunPath.size
        dial.sunPath
            .first()
            .hourText.length shouldBe 2
    }

    @Test
    fun `without a place the heading is magnetic and there are no markers`() {
        val dial = dial(CompassFixtures.settings(place = null), heading = 200.0, snapshot = sky)
        dial.headingText shouldBe "200"
        dial.cardinal shouldBe Cardinal.S
        dial.north shouldBe NorthReference.MAGNETIC
        dial.qibla.shouldBeNull()
        dial.sun.shouldBeNull()
        dial.moon.shouldBeNull()
        dial.placeName.shouldBeNull()
        dial.sunPath.shouldBeEmpty()
    }

    @Test
    fun `texts use the language digits and the announced bucket`() {
        val dial = dial(CompassFixtures.settings("fa"), heading = 116.0, announced = 105)
        dial.headingText shouldBe "۱۲۱"
        dial.announcedDegrees shouldBe 105
        dial.announcedText shouldBe "۱۰۵"
        dial.announcedCardinal shouldBe Cardinal.E
        dial.sunPath
            .first()
            .hourText
            .all { it in '۰'..'۹' } shouldBe true
        CompassStateMapper.degreesText(359.6, NumeralSystem.LATIN) shouldBe "0"
        CompassStateMapper.degreesText(-0.4, NumeralSystem.LATIN) shouldBe "0"
        CompassStateMapper.degreesText(12.5, NumeralSystem.PERSIAN) shouldBe "۱۳"
    }

    @Test
    fun `compass points`() {
        Cardinal.of(0.0) shouldBe Cardinal.N
        Cardinal.of(22.4) shouldBe Cardinal.N
        Cardinal.of(22.5) shouldBe Cardinal.NE
        Cardinal.of(135.0) shouldBe Cardinal.SE
        Cardinal.of(250.0) shouldBe Cardinal.W
        Cardinal.of(337.6) shouldBe Cardinal.N
        Cardinal.of(-45.0) shouldBe Cardinal.NW
        BodyMarker(10f, "10", -1f, "1").aboveHorizon shouldBe false
    }
}
