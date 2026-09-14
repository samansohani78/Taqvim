/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-805 UI: open a month, year paging, calendar switch by swipe and tabs, year selection and pinch zoom. */
@RunWith(RobolectricTestRunner::class)
class YearScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val app = RuntimeEnvironment.getApplication()

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)

    private fun show(navigation: YearNavigation = YearNavigation()): YearViewModel {
        val viewModel =
            YearViewModel(
                FakeYearSettingsSource(PERSIAN_FIRST.copy(languageCode = "en")),
                FakeYearTodaySource(today),
                FakeYearDaysSource(),
            )
        composeRule.setContent { YearTestTheme { YearRoute(navigation = navigation, viewModel = viewModel) } }
        return viewModel
    }

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `tapping a month opens the calendar at its first day`() {
        var opened: Jdn? = null
        show(YearNavigation(onOpenMonth = { opened = it }))
        val farvardin = hasContentDescription("${english.monthNames.persian?.first()} 1405", substring = true)
        awaitNode(farvardin)

        composeRule.onAllNodes(farvardin).onFirst().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { opened == gregorian(2026, 3, 21) }
    }

    @Test
    fun `next and previous change the shown year`() {
        show()
        awaitNode(hasText("1405"))

        composeRule.onNodeWithText(app.getString(R.string.year_next)).performClick()
        awaitNode(hasText("1406"))
        composeRule.onNodeWithText(app.getString(R.string.year_previous)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.year_previous)).performClick()
        awaitNode(hasText("1404"))
        composeRule.onNodeWithText(app.getString(R.string.year_today)).performClick()
        awaitNode(hasText("1405"))
    }

    @Test
    fun `swiping and tabs switch the calendar`() {
        show()
        awaitNode(hasText("1405"))

        composeRule.onNodeWithTag(YEAR_PAGER_TAG).performTouchInput { swipeLeft() }
        awaitNode(hasText("2026"))
        composeRule.onNodeWithText(app.getString(R.string.year_calendar_islamic)).performClick()
        awaitNode(hasText("1447"))
    }

    @Test
    fun `a year chosen in the year selection is shown`() {
        show()
        awaitNode(hasText("1405"))

        composeRule.onNodeWithText(app.getString(R.string.year_choose)).performClick()
        awaitNode(hasTestTag(YEAR_PICKER_TAG))
        composeRule.onNodeWithText("1400").performClick()
        awaitNode(hasText("1400"))
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasTestTag(YEAR_PICKER_TAG)).fetchSemanticsNodes().isEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `pinching and the accessibility actions zoom the grid`() {
        val viewModel = show()
        awaitNode(hasTestTag(YEAR_GRID_TAG))

        composeRule.onAllNodes(hasTestTag(YEAR_GRID_TAG)).onFirst().performTouchInput {
            pinch(center - PINCH_START, center - PINCH_END, center + PINCH_START, center + PINCH_END)
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            (
                viewModel.uiState.value.content
                    ?.columns ?: 0
            ) < YearZoom.DEFAULT_COLUMNS
        }

        repeat(YearZoom.MAX_COLUMNS) {
            composeRule
                .onAllNodes(hasTestTag(YEAR_GRID_TAG))
                .onFirst()
                .performCustomAccessibilityActionWithLabel(app.getString(R.string.year_zoom_out))
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            viewModel.uiState.value.content
                ?.columns == YearZoom.MAX_COLUMNS
        }
    }

    @Test
    fun `a progress indicator is shown until today and the preferences load`() {
        composeRule.setContent { YearTestTheme { YearScreen(YearUiState(), onAction = {}) } }

        composeRule.onNodeWithContentDescription(app.getString(R.string.year_loading)).assertExists()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        val PINCH_START = Offset(20f, 0f)
        val PINCH_END = Offset(300f, 0f)
    }
}
