/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1502 screenshots: city search with the chosen city (light LTR English, dark RTL Persian), the denied permission
 * (light RTL) and typed coordinates with errors and a suggestion (dark LTR). Recorded to
 * `src/test/screenshots/location_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocationSettingsScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureLocationSettings() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val numerals = (if (rtl) LocationFixtures.persian else LocationFixtures.english).numerals
        val chosen = LocationFixtures.choice(LocationFixtures.tehran)
        val base = LocationSettingsUiState(loading = false, current = LocationStateMapper.current(chosen, numerals))
        val state =
            when (sample) {
                "city" -> {
                    base.copy(
                        search =
                            CitySearchState(
                                query = "a",
                                results = LocationStateMapper.rows(LocationFixtures.cities, numerals, chosen),
                            ),
                    )
                }

                "denied" -> {
                    base.copy(mode = LocationMode.DEVICE, device = DeviceState.PermissionDenied)
                }

                else -> {
                    base.copy(
                        mode = LocationMode.COORDINATES,
                        manual =
                            ManualState(
                                latitude = "29.61",
                                longitude = "252.53",
                                timeZoneId = "Asia/Tehran",
                                longitudeError = true,
                                suggestedName = "Shiraz",
                            ),
                    )
                }
            }
        composeRule.captureScreenshot("location_$sample", environment) {
            LocationTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                LocationSettingsScreen(state, LocationSettingsActions())
            }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("city", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("city", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>("denied", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                arrayOf<Any>("coordinates", environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
            )
    }
}
