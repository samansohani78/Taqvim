/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.NumeralSystem.LATIN
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1303 orientation classification, tilt, calibration and ruler ticks. */
class LevelMathTest {
    private val g = 9.81

    private fun rad(degrees: Double) = degrees * PI / 180.0

    @Test
    fun `orientation follows the dominant gravity axis`(): Unit =
        runBlocking {
            LevelMath.classify(Vector3(0.0, 0.0, g)) shouldBe DeviceOrientation.FLAT
            LevelMath.classify(Vector3(0.3, -0.2, -g)) shouldBe DeviceOrientation.FLAT
            LevelMath.classify(Vector3(0.0, g, 0.0)) shouldBe DeviceOrientation.PORTRAIT
            LevelMath.classify(Vector3(1.0, -g, 2.0)) shouldBe DeviceOrientation.PORTRAIT
            LevelMath.classify(Vector3(g, 0.5, 1.0)) shouldBe DeviceOrientation.LANDSCAPE
            LevelMath.classify(Vector3(-g, 0.0, 0.0)) shouldBe DeviceOrientation.LANDSCAPE
            LevelMath.classify(Vector3(5.0, 5.0, 5.0)) shouldBe DeviceOrientation.FLAT
            LevelMath.classify(Vector3(0.0, 0.0, 0.0)).shouldBeNull()
            checkAll(PropertyTesting.iterations, Arb.int(-1000..1000), Arb.int(-1000..1000), Arb.int(-1000..1000)) {
                x,
                y,
                z,
                ->
                val gravity = Vector3(x.toDouble(), y.toDouble(), z.toDouble())
                LevelMath.classify(gravity * 3.5) shouldBe LevelMath.classify(gravity)
            }
        }

    @Test
    fun `tilt of flat and upright devices`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-40..40), Arb.int(-40..40)) { a, b ->
                val alpha = a.toDouble()
                val beta = b.toDouble()
                val flatGravity = Vector3(sin(rad(alpha)) * g, 0.0, cos(rad(alpha)) * g)
                val flat = LevelMath.tilt(flatGravity, DeviceOrientation.FLAT)
                flat.x shouldBe (alpha plusOrMinus 1e-9)
                flat.y shouldBe (0.0 plusOrMinus 1e-9)
                val downGravity = Vector3(0.0, sin(rad(beta)) * g, -cos(rad(beta)) * g)
                val faceDown = LevelMath.tilt(downGravity, DeviceOrientation.FLAT)
                faceDown.y shouldBe (beta plusOrMinus 1e-9)
                val portrait = Vector3(sin(rad(alpha)) * g, cos(rad(alpha)) * g, 0.0)
                LevelMath.tilt(portrait, DeviceOrientation.PORTRAIT).x shouldBe (alpha plusOrMinus 1e-9)
                val upsideDown = Vector3(sin(rad(alpha)) * g, -cos(rad(alpha)) * g, 0.0)
                LevelMath.tilt(upsideDown, DeviceOrientation.PORTRAIT).x shouldBe (alpha plusOrMinus 1e-9)
                val landscape = Vector3(cos(rad(alpha)) * g, sin(rad(alpha)) * g, 0.0)
                LevelMath.tilt(landscape, DeviceOrientation.LANDSCAPE).x shouldBe (alpha plusOrMinus 1e-9)
            }
            val leaning = Vector3(0.0, cos(rad(10.0)) * g, sin(rad(10.0)) * g)
            LevelMath.tilt(leaning, DeviceOrientation.PORTRAIT).y shouldBe (10.0 plusOrMinus 1e-9)
        }

    @Test
    fun `calibration offsets apply per orientation`() {
        val calibration = LevelCalibration(persistentMapOf(DeviceOrientation.FLAT to Tilt(1.0, -0.5)))
        LevelMath.calibrated(Tilt(1.2, -0.4), DeviceOrientation.FLAT, calibration).let {
            it.x shouldBe (0.2 plusOrMinus 1e-9)
            it.y shouldBe (0.1 plusOrMinus 1e-9)
        }
        LevelMath.calibrated(Tilt(1.2, -0.4), DeviceOrientation.PORTRAIT, calibration) shouldBe Tilt(1.2, -0.4)
        LevelMath.isLevel(Tilt(0.5, -0.5), DeviceOrientation.FLAT) shouldBe true
        LevelMath.isLevel(Tilt(0.2, 0.6), DeviceOrientation.FLAT) shouldBe false
        LevelMath.isLevel(Tilt(0.2, 30.0), DeviceOrientation.PORTRAIT) shouldBe true
        LevelMath.isLevel(Tilt(-0.6, 0.0), DeviceOrientation.LANDSCAPE) shouldBe false
    }

    @Test
    fun `ruler ticks from the physical density`() {
        val centimeters = RulerMath.ticks(lengthPx = 205f, dotsPerInch = 254f, unit = RulerUnit.CENTIMETERS)
        centimeters shouldHaveSize 21
        centimeters.filter { it.kind == TickKind.MAJOR }.map { it.positionPx to it.label } shouldBe
            listOf(0f to 0, 100f to 1, 200f to 2)
        centimeters.filter { it.kind == TickKind.MEDIUM }.map { it.positionPx } shouldBe listOf(50f, 150f)
        centimeters.count { it.kind == TickKind.MINOR } shouldBe 16
        val inches = RulerMath.ticks(lengthPx = 160f, dotsPerInch = 160f, unit = RulerUnit.INCHES)
        inches shouldHaveSize 17
        inches.filter { it.kind == TickKind.MAJOR }.map { it.label } shouldBe listOf(0, 1)
        inches.single { it.kind == TickKind.MEDIUM }.positionPx shouldBe 80f
        inches[1].label.shouldBeNull()
        shouldThrow<IllegalArgumentException> { RulerMath.ticks(100f, 0f, RulerUnit.INCHES) }
    }

    @Test
    fun `level content and localized decimals`() {
        LevelStateMapper.content(null, LevelCalibration(), NumeralSystem.LATIN) shouldBe LevelContent.Loading
        LevelStateMapper.content(LevelSample.Unavailable, LevelCalibration(), NumeralSystem.LATIN) shouldBe
            LevelContent.SensorUnavailable
        val tilted = LevelSample.Measured(Vector3(sin(rad(2.0)) * g, 0.0, cos(rad(2.0)) * g), DeviceOrientation.FLAT)
        val reading =
            LevelStateMapper
                .content(tilted, LevelCalibration(), NumeralSystem.LATIN)
                .shouldBeInstanceOf<LevelContent.Reading>()
        reading.xText shouldBe "2.0"
        reading.isLevel shouldBe false
        reading.calibrated shouldBe false
        val zeroed =
            LevelStateMapper
                .content(tilted, LevelCalibration(persistentMapOf(DeviceOrientation.FLAT to tilted.rawTilt)), LATIN)
                .shouldBeInstanceOf<LevelContent.Reading>()
        zeroed.isLevel shouldBe true
        zeroed.calibrated shouldBe true
        LevelStateMapper.oneDecimal(-1.25, NumeralSystem.LATIN) shouldBe "1.3"
        val separator = NumeralSystem.PERSIAN.decimalSeparator
        LevelStateMapper.oneDecimal(12.04, NumeralSystem.PERSIAN) shouldBe "۱۲${separator}۰"
    }
}
