/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayCellSummarizesTheDayAndOffersBothActions() {
        var clicks = 0
        var longClicks = 0
        var tones = emptyList<Color>()
        val model = DayCellModel("1", "Nowruz, holiday", indicators = List(5) { Color.Red }, isSelected = true)
        composeRule.setContent {
            TestTheme {
                val colors = MaterialTheme.colorScheme
                tones = DayTone.entries.map { it.color(colors) }
                Column {
                    DayCell(model, { clicks++ }, Modifier.size(56.dp), { longClicks++ }, "New event")
                    DayCell(DayCellModel("2", "Plain day", isToday = true, isHoliday = true), onClick = {})
                }
            }
        }
        val day = composeRule.onNodeWithContentDescription("Nowruz, holiday")
        day.assertIsSelected().assertHasClickAction().performClick()
        day.performTouchInput { longClick() }
        assertEquals(1, clicks)
        assertEquals(1, longClicks)
        assertEquals("New event", day.fetchSemanticsNode().config[SemanticsActions.OnLongClick].label)
        composeRule.onNodeWithContentDescription("Plain day").assertIsNotSelected()
        composeRule.onNodeWithText("1").assertDoesNotExist()
        assertEquals(4, tones.toSet().size)
    }

    @Test
    fun eventChipsAreDescribedAndClickableOnlyWithAnAction() {
        var clicks = 0
        composeRule.setContent {
            TestTheme {
                Column {
                    EventChip(EventChipModel("Nowruz", Color.Green, "Nowruz, official holiday", isHoliday = true)) {
                        clicks++
                    }
                    EventChip(EventChipModel("Meeting", Color.Blue, "Meeting at ten"))
                }
            }
        }
        composeRule.onNodeWithContentDescription("Nowruz, official holiday").assertHasClickAction().performClick()
        composeRule.onNodeWithContentDescription("Meeting at ten").assertHasNoClickAction()
        composeRule.onNodeWithText("Nowruz").assertDoesNotExist()
        assertEquals(1, clicks)
    }

    @Test
    fun segmentedTabsReportSelection() {
        var selected = 1
        composeRule.setContent {
            TestTheme { SegmentedTabs(listOf("Calendars", "Events", "Times"), selected, { selected = it }) }
        }
        composeRule.onNodeWithText("Events").assertIsSelected()
        composeRule.onNodeWithText("Times").assertIsNotSelected().performClick()
        assertEquals(2, selected)
    }

    @Test
    fun topBarAnnouncesTitleAndLabelsActions() {
        val pressed = mutableListOf<String>()
        composeRule.setContent {
            TestTheme(rtl = true) {
                TopBar(
                    title = "Farvardin 1405",
                    subtitle = "Ramadan 1447",
                    navigation = TopBarAction(TestIcon, "Back", { pressed += "back" }, mirrorInRtl = true),
                    actions = listOf(TopBarAction(TestIcon, "Today", { pressed += "today" })),
                )
            }
        }
        composeRule.onNodeWithText("Farvardin 1405", substring = true).assert(isHeading())
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Today").performClick()
        assertEquals(listOf("back", "today"), pressed)
    }

    @Test
    fun emptyStateAndTooltipCardExposeHeadingsAndActions() {
        val pressed = mutableListOf<String>()
        composeRule.setContent {
            TestTheme {
                Column {
                    EmptyState(
                        "No events",
                        message = "Nothing today",
                        icon = TestIcon,
                        action = ComponentAction("Add") { pressed += "add" },
                    )
                    EmptyState("Bare")
                    TooltipCard(
                        title = "Source",
                        body = "Official calendar of Iran",
                        footnote = "Calendar Center, page 4",
                        action = ComponentAction("Open") { pressed += "open" },
                        dismiss = ComponentAction("Close") { pressed += "close" },
                    )
                    TooltipCard(title = "Hint", body = "Long-press a day")
                }
            }
        }
        composeRule.onNodeWithText("No events").assert(isHeading())
        composeRule.onNodeWithText("Bare").assert(isHeading())
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Source")).assertExists()
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("Calendar Center, page 4").assertExists()
        assertEquals(listOf("add", "open", "close"), pressed)
    }

    @Test
    fun skyGraphicsAreDescribedImagesAndRings() {
        composeRule.setContent {
            TestTheme(rtl = true) {
                Column {
                    MoonDisc(0.3f, waxing = true, "Waxing crescent", Modifier.size(48.dp))
                    MoonDisc(0.8f, waxing = false, "Waning gibbous", Modifier.size(48.dp))
                    SunArc(SunArcModel(0.4f, "06:10", "18:20", "Sun up, 40 percent of daylight"))
                    SunArc(SunArcModel(null, "06:10", "18:20", "Night"))
                    ProgressRing(0.25f, "Countdown", Modifier.size(64.dp)) { Text("12") }
                    ProgressRing(Float.NaN, "Unknown progress", Modifier.size(64.dp))
                }
            }
        }
        composeRule
            .onNodeWithContentDescription("Waxing crescent")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        composeRule.onNodeWithContentDescription("Waning gibbous").assertExists()
        composeRule.onNodeWithContentDescription("Sun up, 40 percent of daylight").assertExists()
        composeRule.onNodeWithText("06:10").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Night").assertExists()
        composeRule.onNodeWithContentDescription("Countdown").assertRangeInfoEquals(ProgressBarRangeInfo(0.25f, 0f..1f))
        composeRule
            .onNodeWithContentDescription("Unknown progress")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun screenSurfaceHostsBarsAndPadsContent() {
        var topPadding = -1f
        composeRule.setContent {
            TestTheme {
                ScreenSurface(topBar = { TopBar("Calendar") }, bottomBar = { Text("Bottom") }) { padding ->
                    topPadding = padding.calculateTopPadding().value
                    Text("Body")
                }
            }
        }
        composeRule.onNodeWithText("Body").assertExists()
        composeRule.onNodeWithText("Bottom").assertExists()
        assertEquals(true, topPadding > 0f)
    }

    @Test
    fun moonLitPathCoversTheLitSide() {
        val center = Offset(50f, 50f)

        fun left(fraction: Float) = moonLitPath(center, 40f, fraction).getBounds().left
        assertEquals(50f, left(0.25f), 0.5f)
        assertEquals(30f, left(0.75f), 0.5f)
        assertEquals(10f, left(1f), 0.5f)
        assertEquals(90f, moonLitPath(center, 40f, 0.5f).getBounds().right, 0.5f)
    }
}
