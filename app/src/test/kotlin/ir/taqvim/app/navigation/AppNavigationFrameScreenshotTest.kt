/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.core.uitesting.ScreenshotDevice
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * ADR-0015 frame: the navigation bar on a phone and the rail on a tablet, light and dark, LTR (English) and RTL
 * (Persian), with the More list as content. Recorded to `src/test/screenshots/app_navigation_frame/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppNavigationFrameScreenshotTest(
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun captureFrame() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings =
            ThemeSettings(
                mode = if (environment.theme.isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                dynamicColor = false,
            )
        composeRule.captureScreenshot("app_navigation_frame", environment) {
            TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR) {
                AppNavigationFrame(TopLevelTab.MORE, onSelect = {}, snackbar = SnackbarHostState()) {
                    MoreScreen(onOpen = {})
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> =
            listOf(
                ScreenshotEnvironment(ScreenshotDevice.PHONE, ScreenshotTheme.LIGHT, LayoutDirection.Ltr),
                ScreenshotEnvironment(ScreenshotDevice.PHONE, ScreenshotTheme.DARK, LayoutDirection.Rtl),
                ScreenshotEnvironment(ScreenshotDevice.TABLET, ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                ScreenshotEnvironment(ScreenshotDevice.TABLET, ScreenshotTheme.DARK, LayoutDirection.Ltr),
            ).map { arrayOf<Any>(it) }
    }
}
