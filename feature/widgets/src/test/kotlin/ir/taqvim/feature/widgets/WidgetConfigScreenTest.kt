/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1200 configuration screen: sections per widget kind and the actions they report. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetConfigScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val summary =
        WidgetConfigUiState(
            kind = WidgetKind.DAY_SUMMARY_2X2,
            config = WidgetConfig.defaultFor(WidgetKind.DAY_SUMMARY_2X2),
            calendars = persistentListOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN),
            loading = false,
        )

    private fun show(
        state: WidgetConfigUiState,
        actions: WidgetConfigActions = WidgetConfigActions(),
    ) {
        composeRule.setContent { TestTheme { WidgetConfigScreen(state, actions) } }
    }

    @Test
    fun everyChoiceIsReported() {
        val calls = mutableListOf<String>()
        show(
            summary,
            WidgetConfigActions(
                onBackground = { calls += "background $it" },
                onTransparency = { calls += "transparency $it" },
                onScale = { calls += "scale $it" },
                onContent = { content, shown -> calls += "content $content $shown" },
                onSecondaryCalendar = { calls += "calendar $it" },
                onSave = { calls += "save" },
                onCancel = { calls += "cancel" },
            ),
        )
        composeRule.onNodeWithText("Black").performClick()
        composeRule.onNodeWithContentDescription("Transparency").performSemanticsAction(SemanticsActions.SetProgress) {
            it(50f)
        }
        composeRule.onNodeWithText("125%").performScrollTo().performClick()
        composeRule.onNodeWithText("Events").performScrollTo().performClick()
        composeRule.onNodeWithText("Gregorian").performScrollTo().performClick()
        composeRule.onNodeWithText("Automatic").performScrollTo().performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(
            listOf(
                "background BLACK",
                "transparency 50",
                "scale 125",
                "content EVENTS false",
                "calendar GREGORIAN",
                "calendar null",
                "save",
                "cancel",
            ),
            calls,
        )
    }

    @Test
    fun theCountdownSectionReportsItsChoices() {
        val calls = mutableListOf<String>()
        val nowruz = WidgetOccasion("Nowruz", CalendarSystem.PERSIAN, 1406, 1, 1, true, "1 Farvardin 1406")
        val countdown = WidgetCountdown(CalendarSystem.PERSIAN, 1405, 6, 22, startJdn = 1)
        val state =
            WidgetConfigUiState(
                kind = WidgetKind.COUNTDOWN,
                config = WidgetConfig.defaultFor(WidgetKind.COUNTDOWN).copy(countdown = countdown),
                loading = false,
                countdown =
                    WidgetCountdownChoices(
                        calendars = persistentListOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN),
                        occasions = persistentListOf(nowruz),
                        monthNames = List(12) { "Month ${it + 1}" }.toImmutableList(),
                        numerals = requireNotNull(LanguageTable.forCode("en")).numerals,
                        years = 1300..1450,
                        dateText = "22 Shahrivar 1405",
                    ),
            )
        show(
            state,
            WidgetConfigActions(
                countdown =
                    WidgetCountdownActions(
                        onMode = { calls += "mode $it" },
                        onTitle = { calls += "title $it" },
                        onCalendar = { calls += "calendar $it" },
                        onRepeats = { calls += "repeats $it" },
                        onOccasion = { calls += "occasion ${it.title}" },
                    ),
            ),
        )
        composeRule.onNodeWithText("Time since").performScrollTo().performClick()
        composeRule.onNodeWithText("Title (optional)").performScrollTo().performTextInput("Trip")
        composeRule.onNodeWithText("Nowruz · 1 Farvardin 1406").performScrollTo().performClick()
        composeRule.onNodeWithText("Gregorian").performScrollTo().performClick()
        composeRule.onNodeWithText("Every year").performScrollTo().performClick()
        composeRule.onNodeWithText("22 Shahrivar 1405").performScrollTo().assertExists()
        composeRule.onNodeWithText("Show").assertDoesNotExist()
        composeRule.onNodeWithText("Change date").performScrollTo().performClick()
        composeRule.onNodeWithText("Choose a date").assertExists()

        assertEquals(
            listOf("mode SINCE", "title Trip", "occasion Nowruz", "calendar GREGORIAN", "repeats true"),
            calls,
        )
    }

    @Test
    fun kindsWithoutOptionalPartsHideThoseSections() {
        show(summary.copy(kind = WidgetKind.MOON, config = WidgetConfig.defaultFor(WidgetKind.MOON)))
        composeRule.onNodeWithText("Background").assertExists()
        composeRule.onNodeWithText("Show").assertDoesNotExist()
        composeRule.onNodeWithText("Second calendar").assertDoesNotExist()
    }

    @Test
    fun loadingAndFailedSaveStates() {
        show(summary.copy(loading = true))
        composeRule.onNodeWithContentDescription("Loading widget settings").assertExists()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun failedSaveIsExplained() {
        show(summary.copy(saveFailed = true))
        composeRule.onNodeWithText("The settings could not be saved. Try again.").assertExists()
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        TaqvimTheme(ThemeSettings(mode = ThemeMode.LIGHT, dynamicColor = false), TextDirection.LTR, content = content)
    }
}
