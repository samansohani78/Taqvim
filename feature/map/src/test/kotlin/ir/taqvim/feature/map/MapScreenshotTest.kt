/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1301 screenshots of six layers on the Natural Earth outline: day and night, Moon visibility, crescent visibility
 * (Yallop, the evening of 2026-08-13, a day after the 12 August new moon), magnetic declination (platform WMM), Qibla
 * and direct path, and the grid, across light/dark and LTR (English)/RTL (Persian). Recorded to
 * `src/test/screenshots/<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val state = state(if (rtl) "fa" else "en")
        composeRule.captureScreenshot(sample, environment) {
            MapTestTheme(rtl = rtl, dark = environment.theme.isDark) { MapScreen(state, MapActions()) }
        }
    }

    private fun state(language: String): MapUiState =
        when (sample) {
            "map_day_night" -> {
                MapFixtures.state(language, setOf(MapLayer.DAY_NIGHT), outline = OUTLINE)
            }

            "map_moon" -> {
                MapFixtures.state(language, setOf(MapLayer.MOON_VISIBILITY), outline = OUTLINE)
            }

            "map_crescent" -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.CRESCENT_VISIBILITY),
                    outline = OUTLINE,
                    instant = Instant.parse("2026-08-13T12:00:00Z"),
                    crescent = CrescentObserver.YALLOP,
                )
            }

            "map_magnetic" -> {
                val layers = setOf(MapLayer.MAGNETIC_DECLINATION)
                MapFixtures.state(language, layers, outline = OUTLINE, magnetic = PlatformMagneticModel)
            }

            "map_paths" -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.QIBLA, MapLayer.DIRECT_PATH),
                    picked = MapFixtures.NEW_YORK,
                    outline = OUTLINE,
                )
            }

            else -> {
                val viewport = MapViewport(zoom = 2.0, centerX = 0.64)
                MapFixtures.state(language, setOf(MapLayer.GRID), outline = OUTLINE, viewport = viewport)
            }
        }

    companion object {
        private val OUTLINE by lazy { MapFixtures.worldOutline() }

        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)
        private val DARK_LTR = environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)
        private val LIGHT_RTL = environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)
        private val DARK_RTL = environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("map_day_night", LIGHT_LTR),
                arrayOf<Any>("map_moon", DARK_RTL),
                arrayOf<Any>("map_crescent", LIGHT_RTL),
                arrayOf<Any>("map_magnetic", DARK_LTR),
                arrayOf<Any>("map_paths", LIGHT_LTR),
                arrayOf<Any>("map_grid", DARK_RTL),
            )
    }
}
