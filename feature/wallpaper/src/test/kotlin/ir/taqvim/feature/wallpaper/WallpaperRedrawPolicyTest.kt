/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.feature.wallpaper.WallpaperFixtures.NOW
import ir.taqvim.feature.wallpaper.WallpaperFixtures.SUNSET
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1215 (U): the wallpaper and daydream redraw only when their content changes, within safe bounds. */
class WallpaperRedrawPolicyTest {
    @Test
    fun `the redraw waits until the content changes`() {
        WallpaperRedrawPolicy.delay(NOW, SUNSET) shouldBe SUNSET - NOW
    }

    @Test
    fun `failed content is retried and past or far changes are bounded`() {
        WallpaperRedrawPolicy.delay(NOW, null) shouldBe WallpaperRedrawPolicy.RETRY
        WallpaperRedrawPolicy.delay(NOW, NOW - 5.seconds) shouldBe WallpaperRedrawPolicy.MIN_DELAY
        WallpaperRedrawPolicy.delay(NOW, NOW + 3.days) shouldBe WallpaperRedrawPolicy.MAX_DELAY
    }

    @Test
    fun `loading failures become null but cancellation is not swallowed`(): Unit =
        runTest {
            WallpaperRedrawPolicy.loadOrNull { 7 } shouldBe 7
            WallpaperRedrawPolicy.loadOrNull<Int> { error("no preferences") } shouldBe null
            shouldThrow<CancellationException> {
                WallpaperRedrawPolicy.loadOrNull {
                    throw CancellationException(
                        "stop",
                    )
                }
            }
        }

    @Test
    fun `property - the delay is always within bounds and exact inside them`(): Unit =
        runTest {
            val offsets = Arb.long(-3 * 86_400L..3 * 86_400L).orNull(0.1)
            checkAll(PropertyTesting.iterations, offsets) { offset ->
                val next = offset?.let { NOW + it.seconds }

                val delay = WallpaperRedrawPolicy.delay(NOW, next)

                (delay >= WallpaperRedrawPolicy.MIN_DELAY && delay <= WallpaperRedrawPolicy.MAX_DELAY) shouldBe true
                val wanted = (next ?: (NOW + WallpaperRedrawPolicy.RETRY)) - NOW
                if (wanted in WallpaperRedrawPolicy.MIN_DELAY..WallpaperRedrawPolicy.MAX_DELAY) delay shouldBe wanted
            }
        }
}
