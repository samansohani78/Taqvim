/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
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
 * search results in Persian, light RTL), the calendar files page after an import, and the official Islamic dates
 * page. Recorded to `src/test/screenshots/settings_<sample>/`.
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
                    "ics_files" -> {
                        IcsFilesScreen(icsFiles, IcsFilesActions(), onPickImport = {}, onPickExport = {})
                    }

                    "islamic_override" -> {
                        IslamicOverrideScreen(islamicOverride(language), IslamicOverrideActions(), onPickFile = {})
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
        /** T-1003: events to export, and the result of an import that left two items unread. */
        private val icsFiles =
            IcsFilesUiState(
                eventCount = 12,
                importOutcome = IcsImportOutcome.Imported(created = 3, replaced = 1, skipped = 2, problems = 2),
            )

        /** ADR-0037: the official Iranian dates in use, with a refused import below. */
        private fun islamicOverride(language: LanguageSpec) =
            IslamicOverrideUiState(
                status =
                    IslamicOverrideStatus(
                        language = language,
                        kind = IslamicOverrideKind.OFFICIAL,
                        firstMonth = CalendarDate(CalendarSystem.ISLAMIC, 1446, 9, 1),
                        lastMonth = CalendarDate(CalendarSystem.ISLAMIC, 1448, 9, 1),
                    ),
                importResult = OverrideImportResult.MISSING_CITATION,
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
                arrayOf<Any>("ics_files", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("ics_files", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>("islamic_override", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("islamic_override", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
            ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("home", "search").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
