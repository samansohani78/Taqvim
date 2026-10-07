/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The year view's text layouts are cached once for the whole pager, never per mini month (T-805).
 *
 * A mini month draws its day numbers through a `TextMeasurer`, whose cache lives as long as the composable that
 * remembers it. While `MiniMonthGrid` remembered its own, each of the twelve months on a page had a cache that died
 * with the page, so paging to another year laid out every day number again: the OnePlus 15 trace of
 * `YearViewBenchmark.pageThroughYears` shows 988–989 `Constructing StaticLayout` slices in each 54–63 ms frame and
 * 0–7 in every frame under 5 ms. Hoisting one cache above the pager took that benchmark's frame CPU P99 from
 * 62.2 ms to 26.9 ms. The day numbers are the same strings in every month and every year, so the cache only works
 * if it outlives a page.
 */
class YearTextCacheKonsistTest {
    @Test
    fun `the year view remembers its text measurer above the pager, not per mini month`() {
        val offenders =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/feature/year/") }
                .filter { it.name != PAGER_FILE }
                .filter { it.text.contains(REMEMBER) }
                .map { it.path }

        offenders.shouldBeEmpty()
    }

    @Test
    fun `the pager is the one place that remembers it`() {
        val holders =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/feature/year/") && it.text.contains(REMEMBER) }
                .map { it.name }

        holders.shouldContainExactly(listOf(PAGER_FILE))
    }

    @Test
    fun `the check would catch a cache remembered inside a mini month`() {
        val perMonth = "@Composable fun Grid() { val m = $REMEMBER(96) }"
        perMonth.contains(REMEMBER) shouldBe true
        "@Composable fun Grid(measurer: TextMeasurer) { }".contains(REMEMBER) shouldBe false
    }

    private companion object {
        const val REMEMBER = "rememberTextMeasurer"

        /** Konsist names a file without its extension. */
        const val PAGER_FILE = "YearPages"
    }
}
