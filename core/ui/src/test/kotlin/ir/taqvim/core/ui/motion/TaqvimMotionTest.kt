/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class TaqvimMotionTest {
    @Test
    fun `durations follow the system animation scale and vanish when animations are off`() {
        TaqvimMotion.scaled(TaqvimMotion.MEDIUM_MILLIS, 1f) shouldBe 300
        TaqvimMotion.scaled(TaqvimMotion.MEDIUM_MILLIS, 0.5f) shouldBe 150
        TaqvimMotion.scaled(TaqvimMotion.LONG_MILLIS, 2f) shouldBe 900
        TaqvimMotion.scaled(TaqvimMotion.SHORT_MILLIS, 0f) shouldBe 0
        TaqvimMotion.scaled(TaqvimMotion.SHORT_MILLIS, -1f) shouldBe 0
        TaqvimMotion.scaled(TaqvimMotion.SHORT_MILLIS, Float.NaN) shouldBe 0
        MotionSettings().reduced shouldBe false
        MotionSettings(0f).reduced shouldBe true
        MotionSettings(Float.NaN).reduced shouldBe true
    }

    @Test
    fun `reduced motion switches screens without any transition`() {
        listOf(true, false).forEach { rtl ->
            val push = NavigationMotion.push(rtl, MotionSettings(0f))
            val pop = NavigationMotion.pop(rtl, MotionSettings(0f))
            push.targetContentEnter shouldBe EnterTransition.None
            push.initialContentExit shouldBe ExitTransition.None
            pop.targetContentEnter shouldBe EnterTransition.None
            pop.initialContentExit shouldBe ExitTransition.None
            NavigationMotion.push(rtl, MotionSettings()).targetContentEnter shouldNotBe EnterTransition.None
            NavigationMotion.pop(rtl, MotionSettings()).initialContentExit shouldNotBe ExitTransition.None
        }
    }

    @Test
    fun `transitions mirror in RTL and pop reverses push`(): Unit =
        runBlocking {
            TaqvimMotion.enterOffsetSign(push = true, rtl = false) shouldBe 1
            TaqvimMotion.enterOffsetSign(push = true, rtl = true) shouldBe -1
            checkAll(PropertyTesting.iterations, Arb.boolean(), Arb.boolean()) { push, rtl ->
                TaqvimMotion.enterOffsetSign(push, rtl) shouldBe -TaqvimMotion.enterOffsetSign(push, !rtl)
                TaqvimMotion.enterOffsetSign(push, rtl) shouldBe -TaqvimMotion.enterOffsetSign(!push, rtl)
            }
        }

    @Test
    fun `shared keys are equal by value and never collide across kinds`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(), Arb.int()) { number, offset ->
                SharedKey.Day(number) shouldBe SharedKey.Day(number)
                SharedKey.Day(number).hashCode() shouldBe SharedKey.Day(number).hashCode()
                SharedKey.Event(number.toString()) shouldNotBe SharedKey.Day(number)
                SharedKey.Month(offset) shouldNotBe SharedKey.Day(offset.toLong())
                SharedKey.Month(offset) shouldNotBe SharedKey.Event(offset.toString())
            }
            val keys =
                (0L until 400L).map { SharedKey.Day(it) } +
                    (0 until 400).map { SharedKey.Month(it) } +
                    (0 until 400).map { SharedKey.Event(it.toString()) }
            keys.toSet() shouldHaveSize keys.size
        }
}
