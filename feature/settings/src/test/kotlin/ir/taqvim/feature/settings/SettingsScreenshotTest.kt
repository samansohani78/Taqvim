/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1500 screenshots: the settings home (interface tab in English, light LTR; location tab in Persian, dark RTL;
 * search results in Persian, light RTL) and the subscriptions page (English, dark LTR). Recorded to
 * `src/test/screenshots/settings_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureSettings() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val language = if (rtl) LocationFixtures.persian else LocationFixtures.english
        val rows = SettingsStateMapper.rows(GeneralSettingsFixtures.settingsFor(language))
        composeRule.captureScreenshot("settings_$sample", environment) {
            LocationTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                when (sample) {
                    "subscriptions" -> {
                        SubscriptionsScreen(subscriptions, SubscriptionsActions())
                    }

                    "search" -> {
                        SettingsHomeScreen(
                            SettingsHomeUiState(false, query = "تقویم", rows = rows),
                            SettingsHomeActions(),
                        )
                    }

                    else -> {
                        val tab = if (rtl) SettingsTab.LOCATION_ATHAN else SettingsTab.INTERFACE_CALENDAR
                        val highlighted = SettingsItemId.PRAYER_METHOD.takeIf { rtl }
                        SettingsHomeScreen(
                            SettingsHomeUiState(false, tab, rows = rows, highlighted = highlighted),
                            SettingsHomeActions(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val subscriptions =
            SubscriptionsUiState(
                loading = false,
                items =
                    persistentListOf(
                        SubscriptionRow(1, "Holidays feed", "https://example.org/holidays.ics", true, true, false),
                        SubscriptionRow(2, "Team", "webcal://example.org/team.ics", false, false, true),
                    ),
                draft = "https://example.org/new.ics",
                message = SubscriptionMessage.ADDED,
            )

        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("home", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("home", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>("search", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                arrayOf<Any>("subscriptions", environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
            ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("home", "search", "subscriptions").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
