/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1501 screenshots of the three onboarding pages: language (light LTR English, dark RTL Persian), location (light RTL
 * Persian, with the T-1502 location settings embedded) and event sources (dark LTR English, light RTL Persian), plus
 * the language and sources pages at font scale 2.0 (T-1701). Recorded to `src/test/screenshots/onboarding_<step>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest(
    private val step: OnboardingStep,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureOnboarding() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val language = if (rtl) LocationFixtures.persian else LocationFixtures.english
        val chosen = setOf(EventSource.INTERNATIONAL) + if (rtl) setOf(EventSource.IRAN_OFFICIAL) else emptySet()
        val state =
            OnboardingUiState(
                loading = false,
                step = step,
                numerals = language.numerals,
                languages =
                    LanguageTable.languages
                        .map { LanguageOption(it.code, it.nativeName, it.code == language.code) }
                        .toImmutableList(),
                sources =
                    SettingsLabels.eventSources
                        .map { (source, label) -> SourceOption(source, label, source in chosen) }
                        .toImmutableList(),
            )
        val place = LocationFixtures.choice(LocationFixtures.tehran)
        val location =
            LocationSettingsUiState(loading = false, current = LocationStateMapper.current(place, language.numerals))
        composeRule.captureScreenshot("onboarding_${step.name.lowercase()}", environment) {
            LocationTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                OnboardingScreen(state, OnboardingActions()) { modifier ->
                    LocationSettingsScreen(location, LocationSettingsActions(), modifier, embedded = true)
                }
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
                arrayOf<Any>(OnboardingStep.LANGUAGE, environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>(OnboardingStep.LANGUAGE, environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>(OnboardingStep.LOCATION, environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                arrayOf<Any>(OnboardingStep.EVENT_SOURCES, environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
                arrayOf<Any>(OnboardingStep.EVENT_SOURCES, environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
            ) +
                listOf(OnboardingStep.LANGUAGE, OnboardingStep.EVENT_SOURCES).flatMap { step ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(step, it) }
                }
    }
}
