/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1301 UI: layer toggles, accessibility zoom and pick actions, tap to pick, the time slider and states. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun show(
        state: MapUiState,
        actions: MapActions = MapActions(),
    ) {
        composeRule.setContent { MapTestTheme { MapScreen(state, actions) } }
    }

    @Test
    fun layerChipsShowAndToggleLayers() {
        val toggled = mutableListOf<MapLayer>()
        show(MapFixtures.state(), MapActions(onToggleLayer = { toggled += it }))
        composeRule.onNodeWithText("World map").assertIsDisplayed()
        composeRule.onNodeWithText("Day and night").performScrollTo().assertIsSelected()
        composeRule
            .onNodeWithText("Moon visibility")
            .performScrollTo()
            .assertIsNotSelected()
            .performClick()
        composeRule.onNodeWithText("Magnetic declination").performScrollTo().performClick()
        assertEquals(listOf(MapLayer.MOON_VISIBILITY, MapLayer.MAGNETIC_DECLINATION), toggled)
    }

    @Test
    fun accessibilityActionsZoomAndPickTheCenter() {
        val calls = mutableListOf<String>()
        val actions =
            MapActions(
                onZoom = { factor, _, size -> calls += "zoom $factor ${size.width > 0f}" },
                onPickCenter = { calls += "center" },
            )
        show(MapFixtures.state(), actions)
        val map = composeRule.onNodeWithContentDescription("World map")
        val custom = map.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle {
            custom.first { it.label == "Zoom in" }.action()
            custom.first { it.label == "Zoom out" }.action()
            custom.first { it.label == "Pick the map center" }.action()
        }
        assertEquals(listOf("zoom 2.0 true", "zoom 0.5 true", "center"), calls)
    }

    @Test
    fun tappingPicksAndTheSliderMovesTheTime() {
        val picks = mutableListOf<ScreenPoint>()
        val minutes = mutableListOf<Int>()
        val days = mutableListOf<Int>()
        val actions =
            MapActions(
                onPick = { point, _ -> picks += point },
                onSelectMinute = { minutes += it },
                onStepDay = { days += it },
            )
        show(MapFixtures.state(picked = MapFixtures.NEW_YORK, layers = setOf(MapLayer.DIRECT_PATH)), actions)
        composeRule.onNodeWithContentDescription("World map").performTouchInput { click(center) }
        composeRule
            .onNodeWithContentDescription("Time of day")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
        composeRule.onNodeWithText("Next day").performClick()
        composeRule.onNodeWithText("Picked point: 40.71°, -74.01°").performScrollTo().assertIsDisplayed()
        assertEquals(1, picks.size)
        assertEquals(listOf(60), minutes)
        assertEquals(listOf(1), days)
    }

    @Test
    fun loadingShowsNoMap() {
        show(MapUiState())
        composeRule.onNodeWithContentDescription("World map").assertDoesNotExist()
        composeRule.onNodeWithText("Day and night").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unavailableOutlineAndMissingPlaceAreExplained() {
        show(MapFixtures.state(place = null).copy(outline = OutlineState.Unavailable))
        composeRule.onNodeWithText("The map could not be loaded").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a place in settings", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun projectionAndCriterionChipsAndLegends() {
        val calls = mutableListOf<Any>()
        val actions = MapActions(onSelectProjection = { calls += it }, onSelectCrescentCriterion = { calls += it })
        val layers =
            setOf(
                MapLayer.CRESCENT_VISIBILITY,
                MapLayer.MAGNETIC_DECLINATION,
                MapLayer.MAGNETIC_INCLINATION,
                MapLayer.MAGNETIC_INTENSITY,
            )
        show(MapFixtures.state(layers = layers, criterion = CrescentCriterion.ODEH), actions)
        composeRule.onNodeWithText("Flat map").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Globe").performScrollTo().performClick()
        composeRule.onNodeWithText("Odeh").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Yallop").performScrollTo().performClick()
        composeRule.onNodeWithText("D: not visible even with optical aid").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("East of true north").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Field points downward").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("60 µT or more").performScrollTo().assertIsDisplayed()
        assertEquals(listOf<Any>(MapProjection.GLOBE, CrescentCriterion.YALLOP), calls)
    }

    @Test
    fun theGlobeTakesTapsAndAccessibilityActions() {
        val calls = mutableListOf<String>()
        val actions = MapActions(onPick = { _, _ -> calls += "pick" }, onPickCenter = { calls += "center" })
        val layers = setOf(MapLayer.DAY_NIGHT, MapLayer.GRID, MapLayer.CITIES, MapLayer.QIBLA)
        show(MapFixtures.state(layers = layers, globe = GlobeView(20.0, 40.0), cities = MapFixtures.cities()), actions)
        val globe = composeRule.onNodeWithContentDescription("World map")
        globe.performTouchInput { click(center) }
        val custom = globe.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle { custom.first { it.label == "Pick the map center" }.action() }
        composeRule.onNodeWithText("Globe").performScrollTo().assertIsSelected()
        assertEquals(listOf("pick", "center"), calls)
    }
}
