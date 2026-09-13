/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotEnvironmentRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appliesDirectionFontScaleDarkModeLocaleAndTheme() {
        val environment =
            ScreenshotEnvironment(ScreenshotDevice.PHONE, ScreenshotTheme.BLACK, LayoutDirection.Rtl, 2.0f)
        var direction: LayoutDirection? = null
        var fontScale = 0f
        var dark = false
        var language = ""
        var theme: ScreenshotTheme? = null

        composeRule.setContent {
            WithScreenshotEnvironment(environment) {
                direction = LocalLayoutDirection.current
                fontScale = LocalDensity.current.fontScale
                dark = isSystemInDarkTheme()
                language = LocalConfiguration.current.locales[0].language
                theme = LocalScreenshotTheme.current
            }
        }
        composeRule.waitForIdle()

        assertEquals(LayoutDirection.Rtl, direction)
        assertEquals(2.0f, fontScale)
        assertEquals(true, dark)
        assertEquals("fa", language)
        assertEquals(ScreenshotTheme.BLACK, theme)
    }

    @Test
    fun capturesScreenshotOnConfiguredDevice() {
        val environment =
            ScreenshotEnvironment(ScreenshotDevice.TABLET, ScreenshotTheme.DARK, LayoutDirection.Ltr, 1.3f)

        composeRule.captureScreenshot("ui_testing_self_test", environment) {
            Box(Modifier.fillMaxSize()) { Text("Screenshot self-test") }
        }

        composeRule.onNodeWithText("Screenshot self-test").assertExists()
        val configuration: Configuration = RuntimeEnvironment.getApplication().resources.configuration
        assertEquals(1280, configuration.screenWidthDp)
        assertEquals(800, configuration.screenHeightDp)
    }
}
