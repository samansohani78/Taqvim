/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.feature.wallpaper.WallpaperFixtures.NOW
import ir.taqvim.feature.wallpaper.WallpaperFixtures.SUNSET
import org.junit.jupiter.api.Test

/** T-1215 (U): frames are drawn only while visible with a surface, and the next one waits for the content to change. */
class WallpaperEngineControllerTest {
    private val ticker = RecordingTicker()
    private var frames = 0
    private val controller = WallpaperEngineController(ticker) { frames++ }

    @Test
    fun `nothing is drawn until the wallpaper is visible and has a surface`() {
        controller.onVisibilityChanged(true)
        frames shouldBe 0
        controller.onSurfaceChanged()
        frames shouldBe 1

        controller.onSurfaceChanged()
        frames shouldBe 2
    }

    @Test
    fun `a drawn frame schedules the next one at the content's change`() {
        controller.onSurfaceChanged()
        controller.onVisibilityChanged(true)
        controller.onFrameDrawn(NOW, SUNSET)

        ticker.delays shouldBe listOf(SUNSET - NOW)
        ticker.fire()
        frames shouldBe 2

        controller.onFrameDrawn(NOW, null)
        ticker.delays.last() shouldBe WallpaperRedrawPolicy.RETRY
    }

    @Test
    fun `a hidden wallpaper holds no timer and draws again when shown`() {
        controller.onSurfaceChanged()
        controller.onVisibilityChanged(true)
        controller.onFrameDrawn(NOW, SUNSET)

        controller.onVisibilityChanged(false)
        ticker.hasPending shouldBe false
        controller.onFrameDrawn(NOW, SUNSET)
        ticker.hasPending shouldBe false
        controller.drawing shouldBe false

        controller.onVisibilityChanged(true)
        frames shouldBe 2
    }

    @Test
    fun `losing the surface or destroying the engine stops drawing`() {
        controller.onVisibilityChanged(true)
        controller.onSurfaceChanged()
        controller.onFrameDrawn(NOW, SUNSET)

        controller.onSurfaceDestroyed()
        ticker.hasPending shouldBe false
        controller.onVisibilityChanged(true)
        frames shouldBe 1

        controller.onSurfaceChanged()
        controller.onDestroy()
        controller.onFrameDrawn(NOW, SUNSET)
        ticker.hasPending shouldBe false
        shouldThrow<IllegalArgumentException> { ticker.fire() }
    }

    @Test
    fun `stars and layouts are stable and inside the surface`() {
        val stars = WallpaperStars.positions(40)
        stars shouldBe WallpaperStars.positions(40)
        stars.filterNot { (x, y) -> x in 0f..1f && y in 0f..1f }.shouldBeEmpty()

        listOf(1080 to 2400, 2400 to 1080, 1 to 1).forEach { (width, height) ->
            val layout = WallpaperLayout.of(width, height)
            listOf(layout.title, layout.moon, layout.month).forEach { box ->
                (box.left >= 0f && box.top >= 0f && box.right <= width && box.bottom <= height) shouldBe true
                (box.width >= 0f && box.height >= 0f) shouldBe true
            }
            (layout.title.bottom <= layout.month.top || width > height) shouldBe true
        }
        shouldThrow<IllegalArgumentException> { WallpaperLayout.of(0, 10) }
    }
}
