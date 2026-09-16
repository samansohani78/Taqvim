/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1000 screenshots: a new event, and a timed monthly event with reminders, a color and a validation error, each in
 * light and dark × LTR (English) and RTL (Persian). Recorded to `src/test/screenshots/event_editor_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EventEditorScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureEditor() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = EditorFixtures.settings(if (rtl) "fa" else "en")
        val new = EditorForm.new(settings, EditorFixtures.TODAY)
        val session =
            when (sample) {
                "new" -> EditorSession.Editing(new, new)
                else -> detailed(new, settings)
            }
        val state = EditorPresenter.present(session, settings, EditorFixtures.TODAY)
        composeRule.captureScreenshot("event_editor_$sample", environment) {
            EditorTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                EventEditorScreen(state, EventEditorActions())
            }
        }
    }

    private fun detailed(
        new: EditorForm,
        settings: EditorSettings,
    ): EditorSession.Editing {
        val repeat =
            RepeatForm
                .create(Frequency.MONTHLY, new.start, settings.language.numerals)
                .copy(invalidDates = InvalidDatePolicy.NEXT_DAY)
        val form =
            new.copy(
                title = "",
                allDay = false,
                colorArgb = EditorPresenter.COLORS[5],
                repeat = repeat,
                reminderMinutes = listOf(15, 1_440),
            )
        return EditorSession.Editing(form, new, showErrors = true)
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val ENVIRONMENTS =
            listOf(
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr),
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                environment(ScreenshotTheme.DARK, LayoutDirection.Rtl),
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf("new", "detailed").flatMap { sample -> ENVIRONMENTS.map { arrayOf<Any>(sample, it) } } +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("new", "detailed").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
