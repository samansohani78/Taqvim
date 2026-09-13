/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.WithScreenshotEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TaqvimThemeRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun providesSchemeTypographyDirectionAndDecoration() {
        val settings = ThemeSettings(mode = ThemeMode.BLACK, dynamicColor = false, gradient = true, boldText = true)
        val image = ImageBitmap(4, 4)
        var scheme: ColorScheme? = null
        var direction: LayoutDirection? = null
        var decoration: ThemeDecoration? = null
        var weight: FontWeight? = null

        composeRule.setContent {
            WithScreenshotEnvironment(ScreenshotEnvironment(theme = ScreenshotTheme.LIGHT)) {
                TaqvimTheme(settings, TextDirection.RTL, backgroundImage = image) {
                    scheme = MaterialTheme.colorScheme
                    direction = LocalLayoutDirection.current
                    decoration = LocalThemeDecoration.current
                    weight = MaterialTheme.typography.bodyLarge.fontWeight
                    TaqvimBackground { }
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color.Black, scheme?.background)
        assertEquals(LayoutDirection.Rtl, direction)
        assertNotNull(decoration?.gradient)
        assertSame(image, decoration?.backgroundImage)
        assertEquals(FontWeight.SemiBold, weight)
    }

    @Test
    fun followsSystemDarkModeAndPlatformDynamicColors() {
        var scheme: ColorScheme? = null
        composeRule.setContent {
            WithScreenshotEnvironment(ScreenshotEnvironment(theme = ScreenshotTheme.DARK)) {
                TaqvimTheme(ThemeSettings(), TextDirection.LTR) { scheme = MaterialTheme.colorScheme }
            }
        }
        composeRule.waitForIdle()

        val schemes = PlatformDynamicSchemes(RuntimeEnvironment.getApplication())
        assertEquals(schemes(true).primary, scheme?.primary)
        assertNotEquals(schemes(true).surface, schemes(false).surface)
    }
}
