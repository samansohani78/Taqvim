/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.ui.unit.LayoutDirection
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ScreenshotMatrixTest {
    @Test
    fun `full matrix covers every theme, direction, font scale and device once`() {
        val matrix = ScreenshotMatrix.full()

        matrix shouldHaveSize 54
        matrix.map { it.id }.toSet() shouldHaveSize 54
        matrix.map { it.device }.toSet() shouldBe ScreenshotDevice.entries.toSet()
        matrix.map { it.theme }.toSet() shouldBe ScreenshotTheme.entries.toSet()
        matrix.map { it.fontScale }.toSet() shouldBe setOf(1.0f, 1.3f, 2.0f)
        ScreenshotMatrix.parameters() shouldHaveSize 54
        ScreenshotMatrix.parameters(matrix.take(2)).map { it.single() } shouldContainExactly matrix.take(2)
    }

    @Test
    fun `devices map to Robolectric qualifiers with plan sizes`() {
        ScreenshotDevice.PHONE.qualifiers shouldBe "w412dp-h915dp-port-mdpi"
        ScreenshotDevice.TABLET.qualifiers shouldBe "w1280dp-h800dp-land-mdpi"
        ScreenshotDevice.FOLD.qualifiers shouldBe "w673dp-h841dp-port-mdpi"
    }

    @Test
    fun `environment ids and defaults are stable`() {
        ScreenshotEnvironment().id shouldBe "phone_light_ltr_fs100_en"
        ScreenshotEnvironment(ScreenshotDevice.FOLD, ScreenshotTheme.BLACK, LayoutDirection.Rtl, 1.3f).id shouldBe
            "fold_black_rtl_fs130_fa"
        ScreenshotTheme.LIGHT.isDark shouldBe false
        ScreenshotTheme.BLACK.isDark shouldBe true
    }

    @Test
    fun `screenshot paths use lower snake case screen names`() {
        screenshotPath("month_grid", ScreenshotEnvironment()) shouldBe
            "src/test/screenshots/month_grid/phone_light_ltr_fs100_en.png"
        shouldThrow<IllegalArgumentException> { screenshotPath("Month Grid", ScreenshotEnvironment()) }
            .message
            .orEmpty() shouldContain "lower_snake_case"
    }
}
