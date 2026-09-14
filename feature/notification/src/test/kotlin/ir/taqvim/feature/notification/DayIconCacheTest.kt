/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import org.junit.runner.RunWith

/** T-1213 (U): day icons are drawn once per number, size, style and color, and old ones are dropped. */
@RunWith(AndroidJUnit4::class)
class DayIconCacheTest {
    @Test
    fun anIconIsDrawnOnceAndReused() {
        val cache = DayIconCache()

        val first = cache.icon("22", 48, DayIconStyle.GLYPH)

        cache.icon("22", 48, DayIconStyle.GLYPH) shouldBeSameInstanceAs first
        cache.drawn shouldBe 1
        first.width shouldBe 48
        first.height shouldBe 48
    }

    @Test
    fun anotherNumberSizeStyleOrColorIsAnotherIcon() {
        val cache = DayIconCache()

        cache.icon("22", 48, DayIconStyle.GLYPH)
        cache.icon("۲۲", 48, DayIconStyle.GLYPH)
        cache.icon("22", 96, DayIconStyle.GLYPH)
        cache.icon("22", 48, DayIconStyle.BADGE, Color.BLUE)
        cache.icon("22", 48, DayIconStyle.BADGE, Color.RED)

        cache.drawn shouldBe 5
    }

    @Test
    fun theLeastRecentlyUsedIconIsDroppedFirst() {
        val cache = DayIconCache(capacity = 2)

        cache.icon("1", 24, DayIconStyle.GLYPH)
        cache.icon("2", 24, DayIconStyle.GLYPH)
        cache.icon("1", 24, DayIconStyle.GLYPH)
        cache.icon("3", 24, DayIconStyle.GLYPH)
        cache.icon("1", 24, DayIconStyle.GLYPH)
        cache.drawn shouldBe 3

        cache.icon("2", 24, DayIconStyle.GLYPH)
        cache.drawn shouldBe 4
    }

    @Test
    fun longNumbersStillFitAndInvalidSizesAreRejected() {
        DayIconCache().icon("12345678", 24, DayIconStyle.BADGE, Color.BLACK).width shouldBe 24

        shouldThrow<IllegalArgumentException> { DayIconCache(capacity = 0) }
        shouldThrow<IllegalArgumentException> { DayIconCache().icon("1", 0, DayIconStyle.GLYPH) }
    }
}
