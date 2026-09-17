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
import kotlinx.collections.immutable.persistentSetOf
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

                    "subscriptions_health" -> {
                        SubscriptionsScreen(health(rtl), SubscriptionsActions())
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

        /** F03: a failed feed with its details open, a stale one and a paused one. */
        private fun health(rtl: Boolean): SubscriptionsUiState =
            SubscriptionsUiState(
                loading = false,
                items =
                    persistentListOf(
                        healthRow(
                            1,
                            "Holidays feed",
                            SubscriptionHealth.FAILED,
                            if (rtl) persianDetails else englishDetails,
                        ),
                        healthRow(2, "Team", SubscriptionHealth.STALE),
                        healthRow(3, "School", SubscriptionHealth.PAUSED),
                    ),
                expanded = persistentSetOf(1L),
            )

        private fun healthRow(
            id: Long,
            name: String,
            health: SubscriptionHealth,
            details: SubscriptionDetails = SubscriptionDetails(),
        ) = SubscriptionRow(
            id = id,
            name = name,
            url = "https://example.org/${name.lowercase().substringBefore(' ')}.ics",
            enabled = health != SubscriptionHealth.PAUSED,
            downloaded = health != SubscriptionHealth.PAUSED,
            refreshing = false,
            health = health,
            details = details,
        )

        private val englishDetails =
            SubscriptionDetails(
                lastSuccess = SubscriptionMoment("Sunday, September 13, 2026", "09:15"),
                lastCheck = SubscriptionMoment("Tuesday, September 15, 2026", "10:30"),
                eventCount = "12",
                cachedFrom = "Saturday, July 11, 2026",
                cachedUntil = "Monday, October 18, 2027",
                problems = "2",
                error = SubscriptionError.SERVER,
                errorStatus = "503",
                errorAt = SubscriptionMoment("Tuesday, September 15, 2026", "10:30"),
            )

        private val persianDetails =
            SubscriptionDetails(
                lastSuccess = SubscriptionMoment("یکشنبه ۲۲ شهریور ۱۴۰۵", "۰۹:۱۵"),
                lastCheck = SubscriptionMoment("سه‌شنبه ۲۴ شهریور ۱۴۰۵", "۱۰:۳۰"),
                eventCount = "۱۲",
                cachedFrom = "شنبه ۲۰ تیر ۱۴۰۵",
                cachedUntil = "دوشنبه ۲۶ مهر ۱۴۰۶",
                problems = "۲",
                error = SubscriptionError.SERVER,
                errorStatus = "۵۰۳",
                errorAt = SubscriptionMoment("سه‌شنبه ۲۴ شهریور ۱۴۰۵", "۱۰:۳۰"),
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
                arrayOf<Any>("subscriptions_health", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("subscriptions_health", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
            ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("home", "search", "subscriptions").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
