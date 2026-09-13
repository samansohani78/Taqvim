/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1302 azimuth from known rotation matrices and sensor readings. */
class RotationMathTest {
    private fun rad(degrees: Double) = degrees * PI / 180.0

    /** device = Rᵀ · world. */
    private fun toDevice(
        rotation: RotationMatrix,
        world: Vector3,
    ): Vector3 =
        Vector3(
            rotation[0] * world.x + rotation[3] * world.y + rotation[6] * world.z,
            rotation[1] * world.x + rotation[4] * world.y + rotation[7] * world.z,
            rotation[2] * world.x + rotation[5] * world.y + rotation[8] * world.z,
        )

    private fun near(
        actual: DeviceAngles,
        azimuth: Double,
        pitch: Double = 0.0,
        roll: Double = 0.0,
    ) {
        abs(Angles.delta(actual.azimuthDegrees, azimuth)) shouldBe (0.0 plusOrMinus 1e-6)
        actual.pitchDegrees shouldBe (pitch plusOrMinus 1e-6)
        actual.rollDegrees shouldBe (roll plusOrMinus 1e-6)
    }

    @Test
    fun `the zero rotation vector is the identity`() {
        val identity = RotationMath.fromRotationVector(Vector3(0.0, 0.0, 0.0))
        identity.rows shouldBe listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
        near(RotationMath.angles(identity), 0.0)
        shouldThrow<IllegalArgumentException> { RotationMatrix(listOf(1.0)) }
    }

    @Test
    fun `turning a flat device counterclockwise lowers its azimuth`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-17_999..18_000)) { hundredths ->
                val heading = hundredths / 100.0
                val rotation = RotationMath.fromRotationVector(Vector3(0.0, 0.0, sin(rad(-heading) / 2)))
                near(RotationMath.angles(rotation), Angles.wrap360(heading))
            }
        }

    @Test
    fun `pitch and roll of rotations about the x and y axes`() {
        val topUp = RotationMath.fromRotationVector(Vector3(sin(rad(30.0) / 2), 0.0, 0.0))
        near(RotationMath.angles(topUp), 0.0, pitch = -30.0)
        val rightDown = RotationMath.fromRotationVector(Vector3(0.0, sin(rad(30.0) / 2), 0.0))
        RotationMath.angles(rightDown).rollDegrees shouldBe (30.0 plusOrMinus 1e-6)
        RotationMath.angles(rightDown).pitchDegrees shouldBe (0.0 plusOrMinus 1e-6)
    }

    @Test
    fun `gravity and field give the four cardinal headings`() {
        val up = Vector3(0.0, 0.0, 9.81)
        val cases =
            mapOf(
                0.0 to Vector3(0.0, 20.0, -40.0),
                90.0 to Vector3(-20.0, 0.0, -40.0),
                180.0 to Vector3(0.0, -20.0, -40.0),
                270.0 to Vector3(20.0, 0.0, -40.0),
            )
        cases.forEach { (heading, field) ->
            near(RotationMath.angles(RotationMath.fromGravityAndField(up, field).shouldNotBeNull()), heading)
        }
    }

    @Test
    fun `both sensor paths agree for tilted devices`(): Unit =
        runBlocking {
            val worldField = Vector3(0.0, 22.0, -41.0)
            val worldUp = Vector3(0.0, 0.0, 9.81)
            checkAll(PropertyTesting.iterations, Arb.int(0..359), Arb.int(-40..40), Arb.int(-40..40)) { h, p, r ->
                val heading = RotationMath.fromRotationVector(Vector3(0.0, 0.0, sin(rad(-h.toDouble()) / 2)))
                val tilt = RotationMath.fromRotationVector(Vector3(sin(rad(p.toDouble()) / 2), 0.0, 0.0))
                val roll = RotationMath.fromRotationVector(Vector3(0.0, sin(rad(r.toDouble()) / 2), 0.0))
                val rotation = multiply(multiply(heading, tilt), roll)
                val fromSensors =
                    RotationMath
                        .fromGravityAndField(toDevice(rotation, worldUp), toDevice(rotation, worldField))
                        .shouldNotBeNull()
                val expected = RotationMath.angles(rotation)
                val actual = RotationMath.angles(fromSensors)
                abs(Angles.delta(actual.azimuthDegrees, expected.azimuthDegrees)) shouldBe (0.0 plusOrMinus 1e-6)
                actual.pitchDegrees shouldBe (expected.pitchDegrees plusOrMinus 1e-6)
                actual.rollDegrees shouldBe (expected.rollDegrees plusOrMinus 1e-6)
            }
        }

    @Test
    fun `degenerate readings have no rotation`() {
        RotationMath.fromGravityAndField(Vector3(0.0, 0.0, 9.81), Vector3(0.0, 0.0, -40.0)).shouldBeNull()
        RotationMath.fromGravityAndField(Vector3(0.0, 0.0, 0.0), Vector3(0.0, 20.0, -40.0)).shouldBeNull()
        RotationMath.fromGravityAndField(Vector3(0.0, 0.0, 9.81), Vector3(0.0, 0.0, 0.0)).shouldBeNull()
    }

    @Test
    fun `vector helpers`() {
        val a = Vector3(1.0, 2.0, 2.0)
        a.norm shouldBe 3.0
        (a cross Vector3(0.0, 0.0, 1.0)) shouldBe Vector3(2.0, -1.0, 0.0)
        Vector3(10.0, 0.0, 0.0).lowPass(Vector3(0.0, 0.0, 0.0), 0.2) shouldBe Vector3(2.0, 0.0, 0.0)
        Vector3(1.0, 1.0, 1.0).lowPass(null, 0.2) shouldBe Vector3(1.0, 1.0, 1.0)
        cos(0.0) shouldBe 1.0
    }

    private fun multiply(
        a: RotationMatrix,
        b: RotationMatrix,
    ): RotationMatrix =
        RotationMatrix(
            (0 until 3).flatMap { row ->
                (0 until 3).map { column -> (0 until 3).sumOf { a[row * 3 + it] * b[it * 3 + column] } }
            },
        )
}
