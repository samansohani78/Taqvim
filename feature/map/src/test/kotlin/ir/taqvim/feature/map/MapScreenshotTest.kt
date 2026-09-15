/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1301 screenshots of the layers on the Natural Earth outline: day and night, Moon visibility, crescent visibility
 * (Yallop and Odeh, the evening of 2026-08-13, a day after the 12 August new moon), magnetic declination, inclination
 * and field strength (platform WMM), Qibla and direct path, the grid, sample city markers and the globe, across
 * light/dark and LTR (English)/RTL (Persian). Recorded to `src/test/screenshots/<sample>/`.
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
                    crescent = CrescentObserver.DEFAULT,
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

            "map_grid" -> {
                val viewport = MapViewport(zoom = 2.0, centerX = 0.64)
                MapFixtures.state(language, setOf(MapLayer.GRID), outline = OUTLINE, viewport = viewport)
            }

            else -> {
                completionState(language)
            }
        }

    /** The samples of the T-1301 completion: Odeh, magnetic inclination and strength, cities and the globe. */
    private fun completionState(language: String): MapUiState =
        when (sample) {
            "map_odeh" -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.CRESCENT_VISIBILITY),
                    outline = OUTLINE,
                    instant = Instant.parse("2026-08-13T12:00:00Z"),
                    crescent = CrescentObserver.DEFAULT,
                    criterion = CrescentCriterion.ODEH,
                )
            }

            "map_inclination" -> {
                val layers = setOf(MapLayer.MAGNETIC_INCLINATION)
                MapFixtures.state(language, layers, outline = OUTLINE, magnetic = PlatformMagneticModel)
            }

            "map_intensity" -> {
                val layers = setOf(MapLayer.MAGNETIC_INTENSITY)
                MapFixtures.state(language, layers, outline = OUTLINE, magnetic = PlatformMagneticModel)
            }

            "map_cities" -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.CITIES, MapLayer.GRID),
                    outline = OUTLINE,
                    viewport = MapViewport(zoom = 4.0, centerX = 0.63, centerY = 0.32),
                    cities = MapFixtures.cities(language),
                )
            }

            "map_globe" -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.DAY_NIGHT, MapLayer.GRID, MapLayer.QIBLA, MapLayer.CITIES),
                    outline = OUTLINE,
                    globe = GlobeView(MapFixtures.TEHRAN.latitude, MapFixtures.TEHRAN.longitude),
                    cities = MapFixtures.cities(language).take(3),
                )
            }

            else -> {
                lineLayerState(language)
            }
        }

    /** DT-035 line layers: Natural Earth time-zone bands and the Matthews et al. (2016) plate boundaries. */
    private fun lineLayerState(language: String): MapUiState =
        when (sample) {
            "map_time_zones" -> {
                MapFixtures.state(language, setOf(MapLayer.TIME_ZONES), outline = OUTLINE)
            }

            "map_plates" -> {
                MapFixtures.state(language, setOf(MapLayer.TECTONIC_PLATES), outline = OUTLINE)
            }

            else -> {
                MapFixtures.state(
                    language,
                    setOf(MapLayer.TECTONIC_PLATES, MapLayer.TIME_ZONES),
                    outline = OUTLINE,
                    globe = GlobeView(MapFixtures.TEHRAN.latitude, MapFixtures.TEHRAN.longitude),
                )
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
            ) +
                // The T-1301 completion: magnetic inclination and strength, cities, the globe and Odeh's criterion.
                listOf("map_inclination", "map_intensity", "map_cities", "map_globe", "map_odeh").flatMap { sample ->
                    listOf(LIGHT_LTR, DARK_LTR, LIGHT_RTL, DARK_RTL).map { arrayOf<Any>(sample, it) }
                } +
                // DT-035: time-zone and plate boundary layers on the flat map, and both on the globe.
                listOf("map_time_zones", "map_plates").flatMap { sample ->
                    listOf(LIGHT_LTR, DARK_LTR, LIGHT_RTL, DARK_RTL).map { arrayOf<Any>(sample, it) }
                } +
                listOf(arrayOf<Any>("map_lines_globe", LIGHT_LTR)) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("map_day_night").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
