/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1302 filter and wrap-around. */
class AnglesTest {
    /** Angles in hundredths of a degree, far beyond one turn in both directions. */
    private val hundredths = Arb.int(-10_000_000..10_000_000)

    @Test
    fun `wrapping stays in one turn and ignores whole turns`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, hundredths, Arb.int(-5..5)) { value, turns ->
                val degrees = value / 100.0
                val wrapped = Angles.wrap360(degrees)
                wrapped shouldBeGreaterThanOrEqual 0.0
                wrapped shouldBeLessThan 360.0
                abs(Angles.delta(wrapped, Angles.wrap360(degrees + 360.0 * turns))) shouldBe (0.0 plusOrMinus 1e-6)
            }
            Angles.wrap360(-1e-17) shouldBe 0.0
            Angles.wrap360(-90.0) shouldBe 270.0
            Angles.wrap360(720.0) shouldBe 0.0
        }

    @Test
    fun `delta is the shortest signed turn`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, hundredths, hundredths) { a, b ->
                val from = a / 100.0
                val to = b / 100.0
                val delta = Angles.delta(from, to)
                delta shouldBeGreaterThanOrEqual -180.0
                delta shouldBeLessThan 180.0
                abs(Angles.delta(Angles.wrap360(from + delta), to)) shouldBe (0.0 plusOrMinus 1e-6)
            }
            Angles.delta(350.0, 10.0) shouldBe (20.0 plusOrMinus 1e-9)
            Angles.delta(10.0, 350.0) shouldBe (-20.0 plusOrMinus 1e-9)
        }

    @Test
    fun `the low-pass filter follows the shorter arc across north`() {
        Angles.lowPass(null, 370.0, 0.1) shouldBe (10.0 plusOrMinus 1e-9)
        Angles.lowPass(350.0, 10.0, 0.5) shouldBe (0.0 plusOrMinus 1e-9)
        Angles.lowPass(10.0, 350.0, 0.25) shouldBe (5.0 plusOrMinus 1e-9)
        Angles.lowPass(90.0, 180.0, 1.0) shouldBe (180.0 plusOrMinus 1e-9)
        var heading: Double? = null
        repeat(200) { index -> heading = Angles.lowPass(heading, if (index % 2 == 0) 358.0 else 2.0, 0.15) }
        abs(Angles.delta(0.0, heading.shouldNotBeNull())) shouldBeLessThanOrEqual 2.0
        shouldThrow<IllegalArgumentException> { Angles.lowPass(0.0, 1.0, 0.0) }
        shouldThrow<IllegalArgumentException> { Angles.lowPass(0.0, 1.0, 1.5) }
    }

    @Test
    fun `the circular mean handles north and opposite directions`() {
        Angles.mean(listOf(350.0, 10.0)).shouldNotBeNull() shouldBe (0.0 plusOrMinus 1e-9)
        Angles.mean(listOf(80.0, 100.0)).shouldNotBeNull() shouldBe (90.0 plusOrMinus 1e-9)
        Angles.mean(listOf(0.0, 180.0)).shouldBeNull()
        Angles.mean(emptyList()).shouldBeNull()
    }

    @Test
    fun `announcements every 15 degrees with hysteresis`(): Unit =
        runBlocking {
            HeadingAnnouncements.next(null, 359.0) shouldBe 0
            HeadingAnnouncements.next(null, 8.0) shouldBe 15
            HeadingAnnouncements.next(0, 8.0) shouldBe 0
            HeadingAnnouncements.next(0, 10.0) shouldBe 0
            HeadingAnnouncements.next(0, 11.0) shouldBe 15
            HeadingAnnouncements.next(15, 4.0) shouldBe 0
            HeadingAnnouncements.next(345, 352.0) shouldBe 345
            HeadingAnnouncements.next(345, 356.0) shouldBe 0
            HeadingAnnouncements.next(90, 200.0) shouldBe 195
            checkAll(PropertyTesting.iterations, Arb.int(0..23), hundredths) { previousIndex, value ->
                val heading = value / 100.0
                val announced = HeadingAnnouncements.next(previousIndex * 15, heading)
                (announced % 15) shouldBe 0
                (announced in 0..345) shouldBe true
                abs(Angles.delta(announced.toDouble(), heading)) shouldBeLessThanOrEqual 10.5
            }
        }
}
